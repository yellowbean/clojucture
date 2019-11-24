(ns clojucture.local.china
  (:require
    [clojucture.asset :as asset]
    [clojucture.account :as acc]
    [clojucture.tranche :as b]
    [clojucture.expense :as exp]
    [clojucture.trigger :as trigger]
    [clojucture.waterfall :as wf]
    [clojucture.pool :as p]
    [clojucture.util :as u]
    [clojucture.spv :as spv]
    [clojucture.reader.base :as rb]
    [java-time :as jt]
    [clojure.core.match :as m]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [medley.core :as mc]
    [clojure.java.io :as io]
    [clojucture.util-cashflow :as uc]
    [com.rpl.specter :as s]
    [com.rpl.specter.zipper :as sz]
    [clojure.zip :as zip]
    [clojure.set :as set])

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





(defn setup-expenses [d u]
  (let [exps (get-in d [:snapshot u :费用])]
    (loop [r {} expense exps]
      (if-let [[k v] (first expense)]
        (recur (assoc r k (exp/setup-expense v)) (next expense))
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

(defn setup-trustee-report [ tr-report ]
  (set/rename-keys tr-report
                   {:新增违约 :new-default-balance}
                   )
  )

(defn setup-trustee-reports [d]
  (->>
      (set/rename-keys d {:受托报告 :trustee-report})
      (s/transform [:trustee-report s/ALL ] setup-trustee-report )
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

         proj-start-index (get-in deal-info [:snapshot u :信息 :当前期数])
         ]
     (-> deal-info
         (assoc-in [:projection :dates] (u/filter-projection-dates dates base-snapshot-date)) ;need to truncate by update date
         (assoc-in [:update :info] {:update-date u})
         (assoc-in [:update :pool] pool)
         (assoc-in [:update :bond] bond)
         (assoc-in [:update :expense] expense)
         (assoc-in [:update :account] accounts)
         (assoc-in [:update :trigger] triggers)
         (assoc-in [:projection :period] (inc proj-start-index))
         (assoc-in [:waterfall] (zip/vector-zip (:分配方式 deal-info))
                   )
         )))
  ([deal-info]
   (let [avail-updates (:snapshot deal-info)
         latest (-> (sort-by jt/local-date (keys avail-updates)) (last))]
     (load-deal deal-info latest))
   )
  )



(defn init-account [deal n]
  (get-in deal [:update :账户 n]))

(defn init-accounts [deal n-list]
  (let [acc-map (get deal [:update :账户])]
    (map #(acc-map %) n-list)))

(defn update-map-by-name [mp new-acc]
  (let [k (:name new-acc)]
    (assoc mp k new-acc)))




(defn run-deal [deal assump]
  "project bond/pool cashflow given assumptions passed in"
  (let [pool (get-in deal [:update :pool])
        coll-dates (get-in deal [:projection :dates :calc-dates])

        pool-cf (.collect-cashflow pool assump coll-dates)
        pool-cf-size (.rowCount pool-cf)

        pay-dates (-> (get-in deal [:projection :dates :pay-dates]) (vec) (subvec 0 pool-cf-size))
        pay-dates-col (u/gen-column {:name :payment-date :type :date :values pay-dates})
        pool-cf-pd (.addColumns pool-cf (into-array AbstractColumn [pay-dates-col])) ; pool cashflow with bond payment dates

        current-wf (deal :waterfall)
        proj-starting-per (get-in deal [:projection :period])

        ;preparation for projection, copy account/triggers/bonds/assets from `update` to `projection`
        deal-for-proj (spv/copy-update-to-proj deal)
        deal-for-proj2 (assoc-in deal-for-proj [:projection :pool-collection] pool-cf-pd)

        ]

    (loop [dp deal-for-proj2 proj-index 0]
      (if (< (get-in dp [:projection :period])  (.rowCount pool-cf-pd))
        (let [pay-date (-> (doto (Row. pool-cf-pd) (.at proj-index)) (.getDate "payment-date"))
              [ update-d run-path ] (wf/walk-waterfall current-wf dp pay-date)]
          (recur update-d (inc proj-index)))
        dp)))
  )

