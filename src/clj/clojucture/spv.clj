(ns clojucture.spv
  (:require [clojucture.bond :as b]
            [clojucture.asset :as a]
            [clojucture.type :as t]
            [clojucture.pool :as p]
            [java-time :as jt]
            [clojucture.util :as u]
            [clojure.java.io :as io]
            [clojure.core.match :as m]
            [clojucture.expense :as exp]
            )
  (:import [tech.tablesaw.api ColumnType Table Row]
           [tech.tablesaw.columns AbstractColumn Column]
           [java.time LocalDate]
           )
  (:use [clojure.core.match.regex])

  )


(defn list-snapshots [d]
  (d :snapshot))



(defn gen-pool-collect-interval [ deal-info ]
  (let [{ closing-date :closing-date
          first-collect-date :first-collect-date
          collect-interval :collect-interval
          stated-maturity :stated-maturity } (:dates deal-info) ]
    (->
      (case collect-interval
        :Q (u/gen-dates-range first-collect-date (jt/months 3) stated-maturity)
        :M (u/gen-dates-range first-collect-date (jt/months 1) stated-maturity)
        :BM (u/gen-dates-range first-collect-date (jt/months 2) stated-maturity)
        :Y (u/gen-dates-range first-collect-date (jt/years 1) stated-maturity))
      (conj closing-date)
      (u/gen-dates-interval )
      )
  )
)

(defn gen-bond-payment-date [ deal-info ]
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
      (conj stated-maturity)(seq))
    )
  )

(defn gen-pool-cf-split-dates [ deal-info ]
  (let [ closing-date (get-in deal-info [:dates :closing-date])
        dd (get-in deal-info [:dates :delay-days])
        split-dates (map #(jt/adjust % jt/minus (jt/days dd))  (deal-info :b-payment-dates)) ]
    (->>
      (cons closing-date split-dates)
      (partition 2 1)) ) )

(defn init-bondflow [ deal-info bonds ]
  (let [ payment-dates (gen-bond-payment-date deal-info)
        default-fields [ [:double :balance] [:double :principal] [:double :interest]
                        [:double :loss] [:double :int] [:double :arrears]]
        tb (u/init-table "Bond Cashflow" default-fields)
        dates-column (into-array AbstractColumn [(u/gen-column [:dates payment-dates])])
        ]
    (.addColumns dates-column tb) ) )

;; simple deal structure without any localization
;; (defrecord deal [ info status pool waterfall bond ])
(defn b-find-cutoff [ d ^LocalDate payment-date ]
  (let [ dd (get-in d [:info :delay-days])]
    
    
    )
  
  )

(defn init-deal [ d ]
  "Popluate deal variables base on deal info ( static data )"
  (when-let [deal d]
    (let [ update-date (get-in deal [:status :update-date])]
      (as-> deal deal-setup
          ;; deal original info
          (assoc-in deal-setup [:info :p-collection-intervals] (gen-pool-collect-interval (deal-setup :info)))
          (assoc-in deal-setup [:info :b-payment-dates] (gen-bond-payment-date (deal-setup :info)))
          (assoc-in deal-setup [:info :p-aggregate-intervals] (gen-pool-cf-split-dates (deal-setup :info)))


          ;; update info
          (assoc-in deal-setup [:status :b-rest-payment-dates]
                    (filter #(jt/after? % update-date) (get-in deal-setup [:info :b-payment-dates])))
          (assoc-in deal-setup [:status :p-rest-collection-intervals]
                    (filter #(jt/after? (first %) update-date) (get-in deal-setup [:info :p-collection-intervals])) )

          ;; update expense
          ;; (assoc-in deal-setup [:status :expense] )
          ;; update pool

          ;; update bond
          ;;

        )
      )
    )
  )


(defn build-deal [ m ]
  (->
    (m/match m
    {:info {:country :China} :status s }
             (assoc m :tax-bond-vat 0.03)
    :else nil
    )
    (init-deal))
  )


(defn run-assets [ d assump ]
  (let [ pool-collect-int (gen-pool-collect-interval (:info d)) ]
    (-> {:deal d}
      (assoc :pool-cf (.collect-cashflow (get-in d [:status :pool]) assump pool-collect-int) )
      (assoc :assumption assump))
  )
)

(defn cn-distribute [ d ^LocalDate dt accounts waterfall expenses bonds ]
  "distribute funds from account to liabilities"
  (loop [ accs accounts ws waterfall exps expenses bnds bonds]
    (if-let [ current-action (first ws)]
      (m/match (:target current-action)
        :增值税 (as->
                (exp/pay-expense-at-base dt ((:source current-action) accs)
                                      ((:target current-action) expenses) 1000  )
                [updated-acc updated-exp]
                (recur updated-acc (next ws) updated-exp bnds ) )
        :else :not-match-ws-action
      )
    )
  ))

(defn choose-distribution-fun [ d ]
  "Pick a distribution function by deal country"
  (m/match d
           {:country :China} cn-distribute
           :else nil
     )
  )

(defn pick-deposit-row [ pool-cf ^LocalDate d]
  (loop [r-flag (Row. pool-cf) ]
    (if-not (or (.hasNext r-flag) nil)
      (.getDate r-flag "ending-date")
      (recur (.next r-flag)  )
      )
    )
  )


(defn run-bonds [ d assump ]
  "projection bond cashflow with pool assumption"
  (let [ bond-rest-payment-dates (get-in d [:info :b-rest-payment-dates ])
        waterfall (get-in d [:info :waterfall])
        agg-mapping (get-in d [:info :deposit-mapping ])
        pool-cf  (:pool-cf (run-assets d assump))
        dist-fun (choose-distribution-fun d)
        current-bonds (get-in d [:status :bond])
        current-expense (get-in d [:status :expense])
        current-accounts (get-in d [:status :account])
        ;trial-table (doto (Table/create "Trial") (.addColumns (.column pool-cf "dates")))
        ]
    (loop [ pay-dates bond-rest-payment-dates exps current-expense bnds current-bonds accs current-accounts ]
      ;(println pay-dates)
      (if-let [ current-pay-date (first pay-dates)]
        (let [ deposit-row (pick-deposit-row pool-cf current-pay-date)
              ;_ (println deposit-row)
              accs-with-deposit (p/deposit-period-to-accounts deposit-row accs agg-mapping current-pay-date )
              [ update-accounts update-bonds update-expenses ] (dist-fun d current-pay-date accs-with-deposit waterfall exps bnds)
              ]
          (recur
            (next pay-dates)
            update-expenses
            update-bonds
            update-accounts )
            )
        [bnds exps accs] )
      ))
)
