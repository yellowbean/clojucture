(ns clojucture.spv
  (:require [clojure.edn :as edn]
            [clojucture.tranche :as b]
            [clojucture.asset :as a]
            [clojucture.account :as acc]
            [clojucture.pool :as p]
            [java-time :as jt]
            [clojucture.util :as u]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.core.match :as m]
            [com.rpl.specter :as s]
            [com.rpl.specter.macros :as sm]
            [infix.macros :as mc ]
            )
  (:import [tech.tablesaw.api ColumnType Table Row]
           [tech.tablesaw.columns AbstractColumn Column]
           [java.time LocalDate]
           )
  (:use [clojure.core.match.regex])

  )


(defn list-snapshots [d]
  "list snapshot : d -> deal map"
  (d :snapshot))



(defn gen-pool-collect-interval [deal-info]
  (let [{closing-date       :closing-date
         first-collect-date :first-collect-date
         collect-interval   :collect-interval
         stated-maturity    :stated-maturity} (:dates deal-info)]
    (->
      (case collect-interval
        :Q (u/gen-dates-range first-collect-date (jt/months 3) stated-maturity)
        :M (u/gen-dates-range first-collect-date (jt/months 1) stated-maturity)
        :BM (u/gen-dates-range first-collect-date (jt/months 2) stated-maturity)
        :Y (u/gen-dates-range first-collect-date (jt/years 1) stated-maturity))
      (conj closing-date)
      (u/gen-dates-interval)
      )
    )
  )

(defn gen-bond-payment-date [deal-info]
  (let [{
         first-payment-date :first-payment-date
         payment-interval   :payment-interval
         stated-maturity    :stated-maturity} (:dates deal-info)]
    (->
      (case payment-interval
        :Q (u/gen-dates-range first-payment-date (jt/months 3) stated-maturity)
        :M (u/gen-dates-range first-payment-date (jt/months 1) stated-maturity)
        :BM (u/gen-dates-range first-payment-date (jt/months 2) stated-maturity)
        :Y (u/gen-dates-range first-payment-date (jt/years 1) stated-maturity))
      (vec)
      (conj stated-maturity) (seq))
    )
  )

(defn gen-pool-cf-split-dates [deal-info]
  (let [closing-date (get-in deal-info [:dates :closing-date])
        dd (get-in deal-info [:dates :delay-days])
        split-dates (map #(jt/adjust % jt/minus (jt/days dd)) (deal-info :b-payment-dates))]
    (->>
      (cons closing-date split-dates)
      (partition 2 1))))

(defn init-bondflow [deal-info bonds]
  (let [payment-dates (gen-bond-payment-date deal-info)
        default-fields [[:double :balance] [:double :principal] [:double :interest]
                        [:double :loss] [:double :int] [:double :arrears]]
        tb (u/init-table "Bond Cashflow" default-fields)
        dates-column (into-array AbstractColumn [(u/gen-column [:dates payment-dates])])
        ]
    (.addColumns dates-column tb)))


(defn init-deal [d]
  "Popluate deal variables base on deal info ( static data )"
  (when-let [deal d]
    (let [update-date (get-in deal [:status :update-date])]
      (as-> deal deal-setup
            ;; deal original info
            (assoc-in deal-setup [:info :p-collection-intervals] (gen-pool-collect-interval (deal-setup :info)))
            (assoc-in deal-setup [:info :b-payment-dates] (gen-bond-payment-date (deal-setup :info)))
            (assoc-in deal-setup [:info :p-aggregate-intervals] (gen-pool-cf-split-dates (deal-setup :info)))


            ;; update info
            (assoc-in deal-setup [:status :b-rest-payment-dates]
                      (filter #(jt/after? % update-date) (get-in deal-setup [:info :b-payment-dates])))
            (assoc-in deal-setup [:status :p-rest-collection-intervals]
                      (filter #(jt/after? (first %) update-date) (get-in deal-setup [:info :p-collection-intervals])))

            ;; update expense
            ;; (assoc-in deal-setup [:status :expense] )
            ;; update pool

            ;; update bond
            ;;

            )
      )
    )
  )


;; return available of assumptions given a deal instance

(defn gen-assumptions [x]
  (m/match x

           )
  )



(defn run-assets [d assump]
  (let [pool-collect-int (gen-pool-collect-interval (:info d))]
    (-> {:deal d}
        (assoc :pool-cf (.collect-cashflow (get-in d [:status :pool]) assump pool-collect-int))
        (assoc :assumption assump))
    )
  )



(defn pick-deposit-row [pool-cf ^LocalDate d]
  (loop [r-flag (Row. pool-cf)]
    (if-not (or (.hasNext r-flag) nil)
      (.getDate r-flag "ending-date")
      (recur (.next r-flag))
      )
    )
  )


(defn- choose-distribution-fun [ d ]
  nil
  )


(defn run-bonds [d assump]
  "projection bond cashflow with pool assumption"
  (let [bond-rest-payment-dates (get-in d [:info :b-rest-payment-dates])
        waterfall (get-in d [:info :waterfall])
        agg-mapping (get-in d [:info :deposit-mapping])
        pool-cf (:pool-cf (run-assets d assump))
        dist-fun (choose-distribution-fun d)
        current-bonds (get-in d [:status :bond])
        current-expense (get-in d [:status :expense])
        current-accounts (get-in d [:status :account])
        ]
    (loop [pay-dates bond-rest-payment-dates exps current-expense bnds current-bonds accs current-accounts]
      (if-let [current-pay-date (first pay-dates)]
        (let [deposit-row (pick-deposit-row pool-cf current-pay-date)
              accs-with-deposit (p/deposit-period-to-accounts deposit-row accs agg-mapping current-pay-date)
              [update-accounts update-bonds update-expenses] (dist-fun d current-pay-date accs-with-deposit waterfall exps bnds)
              ]
          (recur
            (next pay-dates)
            update-expenses
            update-bonds
            update-accounts)
          )
        [bnds exps accs])
      ))
  )



(defn get-pool-collection
  ([d ^Integer per]
   (let [ pool-cf (get-in d [:projection :pool-collection]) ]
     (if (> per  (.rowCount pool-cf))
       (throw (Exception.  (str "Invalid Per for Pool Size" per ", pool cf row count " (.rowCount pool-cf)  )))
       (doto (Row. pool-cf) (.at per)))))

  ([d ^Integer per field field-type]
   (let [pc (get-pool-collection d per)]
     (case field-type
       :double (.getDouble pc (name field))
       :date (.getDate pc (name field))
       ))))

(defn query-deal [d e]
  "query deal statistic by expression "
  ;(prn "Matching " e)                                       ;
  (m/match e
           [:projection :bond :sum-current-balance]
           (reduce + 0 (s/select [:projection :bond s/ALL :balance] d))

           [:projection :pool :sum-current-balance]
           (reduce + 0 (s/select [:projection :pool :assets s/ALL :current-balance] d))

           [:projection :trigger trg :status]
           (:status (s/select-one [:projection :trigger trg] d))

           [:update :bond :sum-current-balance]
           (reduce + 0 (s/select [:update :bond s/ALL s/LAST :balance] d))

           [:update :pool :sum-current-balance]
           (reduce + 0 (s/select [:update :pool :assets s/ALL :current-balance] d))

           [:trustee-report :sum-new-default-current-balance]
           (reduce + 0 (s/select [:trustee-report s/ALL :new-default-balance] d))

           [:current-collection (c-field-list :guard list?)]
           (map #(query-deal d [:current-collection %]) c-field-list)

           [:current-collection :sum-new-default-current-balance]
           (get-pool-collection d (get-in d [:projection :period]) :default :double)

           [:current-collection c-field]
           (get-pool-collection d (get-in d [:projection :period]) c-field :double)


           [:post-collection :sum-new-default-current-balance] ; history + projection
           (let [history-new-default-bal (query-deal d [:trustee-report :sum-new-default-current-balance] )
                 current-deal-per (get-in d [:projection :period])
                 proj-new-default-bal (reduce +
                                              (for [x (range 1 current-deal-per)]
                                                (get-pool-collection d x :default :double)))]
             (+ proj-new-default-bal history-new-default-bal))

           [:post-dist source-key :transfer-to target-key :sum-amount] ;在以往 所有的“信托分配日”按照信托合同第 9.2(b)(i)项已从“本金分账户”转至“收 入分账户”的金额
           (let [acc (s/select-one [:projection :account source-key] d)
                 selected-stmts (acc/select-stmts acc {:to target-key})]
             (reduce +
                     (s/select [s/ALL :amount] selected-stmts))
             )


           [:post-dist source-key :transfer-to target-key :net-amount] ;在以往 所有的“信托分配日”按照信托合同第 9.2(b)(i)项已从“本金分账户”转至“收 入分账户”的金额
           (let [acc (s/select-one [:projection :account source-key] d)
                 to-stmts (acc/select-stmts acc {:to target-key})
                 from-stmts (acc/select-stmts acc {:from target-key})]
             (- (acc/sum-stmts to-stmts) (acc/sum-stmts from-stmts)))

           :else nil
           ;:else (do (prn "Not match expression: " e ) (throw (Exception. "Not match query ")) )

           ))


(defn- repl-formula [s x]
  (str/replace-first s #"\[.*?\]" (str "(" (format "%.8f" (double x)) ")")) )

(defn eval-formula [fml-s vs]
  (mc/from-string
    (reduce repl-formula fml-s vs)))

(defn evaluate-formula [d fml]
  "evaluate formula: d -> deal map , fml -> string formula"
  (let [fml-t (-> (str/replace fml #"\n" "") )
        deal-vars-strs (map read-string (re-seq #"\[.*?\]" fml-t))
        deal-vars (map #(query-deal d %) deal-vars-strs)
      fml-v (eval-formula fml-t deal-vars)]
    (fml-v)))


(defn copy-update-to-proj [d]
  (let [cpy-fn (fn [v f] (assoc-in v [:projection f] (get-in v [:update f])))]
    (reduce cpy-fn d [:account :expense :trigger :bond :pool])))