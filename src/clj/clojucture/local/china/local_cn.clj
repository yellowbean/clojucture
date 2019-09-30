(ns clojucture.local.china.local_cn
  (:require
    [clojucture.asset :as asset]
    [clojucture.account :as acc]
    [clojucture.tranche :as b]
    [clojucture.expense :as exp]
    [clojucture.trigger :as trigger]
    [clojucture.pool :as p]
    [clojucture.util :as u]
    [clojucture.reader.base :as rb]
    [java-time :as jt]
    [clojure.core.match :as m]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [medley.core :as mc]
    [clojure.java.io :as io])

  (:use [clojure.core.match.regex])
  (:import [java.time LocalDate]
           [clojucture.trigger pool-trigger]
           (tech.tablesaw.api Row)
           (tech.tablesaw.columns AbstractColumn)
           )
  )




(defn setup-accounts [d u]
  "d -> deal input map, u -> deal update"
  (loop [accs {} accs-to-add (get-in d [:snapshot u :账户])]
    (if (nil? accs-to-add)
      accs
      (let [[k v] (first accs-to-add)]
        (recur
          (assoc accs
            k
            (m/match v
                     {:名称 acc-name} (acc/->account acc-name nil 0 [])
                     :else nil))
          (next accs-to-add))))))


(defn setup-asset-mortgage [a]
  (m/match a
           {:初始面额 original-bal :当前面额 current-bal :年利率 annual-rate :摊销 amort-type :初始日 start-date
            :支付日期 pay-dates :初始期限 original-term :当前期限 remain-term}
           (asset/->mortgage
             {:start-date  (rb/parsing-dates start-date) :balance original-bal :periodicity (jt/months 1) :term original-term
              :period-rate (/ annual-rate 12)}
             nil current-bal (/ annual-rate 12) remain-term nil)


           :else nil
           )

  )

(defn setup-assets [d u]
  (let [assets (get-in d [:snapshot u :资产池 :资产清单])
        coll-type (get-in d [:snapshot u :资产池 :类型])]
    (m/match coll-type
             :住房按揭 (map setup-asset-mortgage assets)
             :else nil)
    ))

(defn setup-pool [d u]
  (let [assets (setup-assets d u)
        cut-off-date (-> (get-in d [:snapshot u :资产池 :封包日]) (rb/parsing-dates))
        ]
    (p/->pool assets cut-off-date)

    ))


(defn setup-dates [d]
  (let [cut-off-date (jt/local-date (get-in d [:日期 :初始起算日]))
        stated-maturity-date (get-in d [:日期 :法定到期日])]
    {
     :cut-off-date    cut-off-date
     :stated-maturity stated-maturity-date
     :pay-dates       (rb/parsing-dates (str (get-in d [:日期 :支付日]) "," stated-maturity-date))
     :int-dates       (rb/parsing-dates (str (get-in d [:日期 :计息日]) "," stated-maturity-date))
     :calc-dates      (cons cut-off-date (rb/parsing-dates (str (get-in d [:日期 :计算日]) "," stated-maturity-date)))
     :dist-dates      (rb/parsing-dates (str (get-in d [:日期 :信托分配日]) "," stated-maturity-date))
     }
    )
  )


(defn setup-expense [x]
  (m/match x
           {:name n :last-paid-date pd :year-rate r :arrears ars}
           (exp/->pct-expense-by-amount {:name n :pct r :day-count :ACT_365} nil pd ars)
           {:name n :balance v :last-paid-date pd}
           (exp/->amount-expense {:name n} nil pd v)
           {:name n :last-paid-date pd :base-rate r :arrears ars}
           (exp/->pct-expense-by-rate {:name n :pct r} nil pd ars)
           :else :not-match-expense
           )
  )


(defn setup-expenses [d u]
  (let [exps (get-in d [:snapshot u :费用])]
    (loop [r {} expense exps]
      (if-let [[k v] (first expense)]
        (recur (assoc r k (setup-expense v)) (next expense))
        r))))

(defn setup-trigger [x]
  (m/match x
           {:name n :cond [:资产池违约率 :大于 th]}
           (trigger/pool-trigger. n {:target :pool-cumulative-default-rate :op > :threshold th :curable false} false)
           ;(trigger/->trigger {:name n :watch :pool-cumulative-default-rate} > th)


           :else :not-match-trigger))

(defn setup-triggers [d u]
  (let [trgs (get-in d [:snapshot u :风险事件])]
    (mc/map-vals setup-trigger trgs)))

(defn setup-bond [bnd]
  (m/match bnd
           {:简称    label :初始面额 orig-bal :当前余额 cur-bal :执行利率 cur-rate :预期到期日 expected-date
            :法定到期日 stated-maturity-date :上次付款日 {:principal prin-last-paid-date :interest int-last-paid-date}}
           (b/->sequence-bond {:day-count :ACT_365} cur-bal cur-rate []
                              {:principal (rb/parsing-dates prin-last-paid-date) :interest (rb/parsing-dates int-last-paid-date)} 0 0)
           {:简称    label :初始面额 orig-bal :当前余额 cur-bal :预期到期日 expected-date
            :法定到期日 stated-maturity-date :上次付款日 {:principal prin-last-paid-date :interest int-last-paid-date}}
           (b/->equity-bond {:day-count :ACT_365} cur-bal [] {:principal (rb/parsing-dates prin-last-paid-date) :interest (rb/parsing-dates int-last-paid-date)})
           :else :not-match-bond-map
           ))


(defn setup-bonds [d u]
  (let [bnds (get-in d [:snapshot u :债券])]
    (loop [r {} bonds bnds]
      (if-let [[k v] (first bonds)]
        (recur (assoc r k (setup-bond v)) (next bonds))
        r))))


(defn filter-projection-dates [ds ^LocalDate sp-date]
  (let [dates-after-sp-date (fn [x] (if (instance? clojure.lang.PersistentVector x) (filter (partial jt/after? sp-date) x) x))]
    (reduce-kv #(assoc %1 %2 (dates-after-sp-date %3)) {} ds)
    ))




(defn load-deal
  ([deal-info u]
  (let [; initialized deal from a clojure map
        accounts (setup-accounts deal-info u)
        dates (setup-dates deal-info)
        pool (setup-pool deal-info u)
        bond (setup-bonds deal-info u)
        triggers (setup-triggers deal-info u)
        expense (setup-expenses deal-info u)
        base-snapshot-date (jt/local-date u)
        ]
    (-> deal-info
        (assoc-in [:projection :dates] (filter-projection-dates dates base-snapshot-date)) ;need to trancate by update date
        (assoc-in [:update :info] {:update-date u})
        (assoc-in [:update :资产池] pool)
        (assoc-in [:update :债券] bond)
        (assoc-in [:update :税费] expense)
        (assoc-in [:update :账户] accounts)
        (assoc-in [:update :事件] triggers)
        )
    ))
  ([ deal-info ]
   (let [ avail-updates (:snapshot deal-info)
         latest (-> (sort-by jt/local-date (keys avail-updates)) (last)) ]
     (load-deal deal-info latest) )
    )
  )

(defn run-pool [pool assump coll-int]
  (.collect-cashflow pool assump coll-int))

(defn init-account [deal n]
  (get-in deal [:update :账户 n]))

(defn init-accounts [deal n-list]
  (let [acc-map (get deal [:update :账户])]
    (map #(acc-map %) n-list)))

(defn update-map-by-name [mp new-acc]
  (let [k (:name new-acc)]
    (assoc mp k new-acc)))

(defn update-map-with-list [mp i-list]
  (reduce (fn [x y] (update-map-by-name x y)) mp i-list))

(defn distribute-funds [ pool-collection-with-pd proj-period accs exps bnds trgs dist-actions]
  (let [fee? (fn [x] (contains? exps x))
        account? (fn [x] (contains? accs x))
        all-account? (fn [x] (every? account? x))
        trigger? (fn [x] (contains? trgs x))
        bond? (fn [x] (contains? bnds x))

        pool-collection (doto (Row. pool-collection-with-pd) (.at proj-period))
        pay-date (.getDate pool-collection "payment-date")


        get-pc-field (fn [ pc f ] (.getDouble pc f) )
        split-obj (fn [x] (-> x (name) (str/split #"\.") (first) (keyword)))

        ]
    (loop [actions dist-actions accounts accs expenses exps bonds bnds]
      (if-let [action (first actions)]
        (as->
          (m/match action
                   {:from pool-c :to t-cc :fields f-list}   ;:to target-acc :fields f-list }
                   (let [_ (prn (.columnNames pool-collection))
                         amounts-to-deposit (map #(.getDouble pool-collection %) f-list)
                         acc-to-dep (accs t-cc)
                         acc-u (.deposit acc-to-dep pay-date :pool-collection (reduce + amounts-to-deposit))
                         ]
                     [(update-map-with-list accounts [acc-u]) expenses bonds]
                     )

                   {:from source-acc :to target-acc :formula fml}
                   (let [ a (get-pc-field pool-collection "default")
                         b nil
                         c nil
                         d nil

                         ]

                     [accounts expenses bonds]
                     )

                   {:if trig-key :breached sub-actions-t :unbreach sub-actions-f}
                   (let [test-trigger (trgs trig-key)]
                     (if (:status test-trigger)
                       (distribute-funds pay-date pool-collection accounts expenses bonds trgs sub-actions-t)
                       (distribute-funds pay-date pool-collection accounts expenses bonds trgs sub-actions-f)))

                   {:from source-acc :to target-acc :expense (obj :guard fee?) :with-limit-percentage pct}
                   (let [exp (expenses obj)]
                     [accounts expenses bonds]
                     )
                   {:from source-acc :to target-acc :expense (obj :guard seqable?)}
                   (let []
                     [accounts expenses bonds]
                     )
                   {:from source-acc :to target-acc :expense (obj :guard fee?)}
                   (let [fee-to-pay (expenses obj)
                         acc-to-pay (accounts source-acc)

                         ]
                     [accounts expenses bonds]
                     )

                   {:from source-acc :to target-acc :interest (obj :guard seqable?)}
                   (let [acc-to-pay (accounts source-acc)
                         objs-to-pay (map split-obj obj)
                         bonds-to-pay (map bonds objs-to-pay)
                         [acc-u bnd-u] (b/pay-bond-interest-pr pay-date acc-to-pay bonds-to-pay)
                         ]
                     [(update-map-with-list accounts [acc-u]) expenses (update-map-with-list bonds [bnd-u])]
                     )

                   {:from source-acc :to target-acc :interest (obj :guard bond?)  }
                   (let [ b-name (split-obj obj)
                         bond-to-pay (b-name bonds)
                         s-acc (accounts source-acc)
                         [acc-u bond-u] (b/pay-bond-interest pay-date s-acc bond-to-pay)
                         ]
                     [(assoc accounts source-acc acc-u) expenses (assoc bonds b-name bond-u)]
                     )



                   {:from source-acc :to target-acc :principal (obj :guard bond?) }
                   (let [;s (name obj)
                         b-name (split-obj obj)
                         s-acc (accounts source-acc)
                         bond-to-pay (-> (keyword b-name) (bonds))
                         [acc-u bond-u] (b/pay-bond-principal pay-date s-acc bond-to-pay)
                         ]
                     [ (assoc accounts source-acc acc-u) expenses (assoc bonds b-name bond-u)]
                     )

                   {:from (source-acc :guard account?) :to (target-acc :guard account?)}
                   (let [[s-acc t-acc] (acc/transfer-fund (accounts source-acc) (accounts target-acc) pay-date)
                         up-accs (update-map-with-list accounts [s-acc t-acc])
                         ]
                     [up-accs expenses bonds]
                     )

                   {:from (acc-key-list :guard all-account?) :to (acc-key :guard account?)}
                   (let [source-acc-list (-> (select-keys accounts acc-key-list) (vals))
                         target-acc (accounts acc-key)
                         [update-s-accs update-t-acc] (acc/transfer-funds source-acc-list target-acc pay-date)
                         up-accs (update-map-with-list accounts (conj update-s-accs update-t-acc))]
                     [up-accs expenses bonds])

                   {:from (source-acc :guard account?) :to (target-acc :guard account?) :amount (amt :guard number?)}
                   (let [[s-acc t-acc] (acc/transfer-fund (accounts source-acc) (accounts target-acc) pay-date amt)
                         up-accs (update-map-with-list accounts [s-acc t-acc])]
                     [up-accs expenses bonds])

                   :else (throw (Exception. "NOT MATCH"))   ;[ accounts expenses bonds ]
                   )
          [updated-accs updated-exps updated-bnds]
          (recur (next actions) updated-accs updated-exps updated-bnds))
        [accounts expenses bonds trgs]
        )
      )
    )
  )


(defn run-deal [ deal assump ]
  (let [pool (get-in deal [:update :资产池])
        coll-dates (get-in deal [:projection :dates :calc-dates])

        pool-cf (run-pool pool assump coll-dates)

        pay-dates (-> (get-in deal [:projection :dates :pay-dates]) (u/dates))
        pay-dates-col (u/gen-column [:payment-date  pay-dates])
        pool-cf-pd (.addColumns pool-cf (into-array AbstractColumn [pay-dates-col])) ; pool cashflow with bond payment dates

        wf (:分配方式 deal)

        bnds (get-in deal [:update :债券])
        accs (get-in deal [:update :账户])
        exps (get-in deal [:update :税费])
        trgs (get-in deal [:update :事件])
        ]
    (loop [ bonds bnds accounts accs expenses exps triggers trgs proj-index 0]
      (if (<= proj-index (.rowCount pool-cf-pd) )
        (let [ eod (:违约事件 triggers)
              current-wf (if (:status eod) (:违约后 wf) (:违约前 wf))
              [acc-u exp-u bnd-u trg-u] (distribute-funds pool-cf-pd proj-index  accounts expenses bonds triggers current-wf)]
          (recur bnd-u acc-u exp-u trg-u (inc proj-index)))
        (-> deal
            (assoc-in [:projection :bond] bonds)
            (assoc-in [:projection :account] accounts)
            (assoc-in [:projection :expense] expenses)
            (assoc-in [:projection :triggers] triggers)
            )
        )
      )
    )
  )

(defn dump-deal [ d ]
  (let [ d-map (into {} d)]

    )
  )
