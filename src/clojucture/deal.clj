(ns clojucture.deal
  (:require [clojucture.bond :as b]
            [clojucture.asset :as a]
            [clojucture.type :as t]
            ;[clojucture.reader :as r]
            [java-time :as jt]
            [clojucture.util :as u]
            [clojure.java.io :as io])
  (:import [tech.tablesaw.api ColumnType Table]
           [tech.tablesaw.columns AbstractColumn Column]
           )

  )


(defrecord china-bank-deal [ deal-info opt ]
  t/Deal
  (run-assets [ x assump ]
    (let [
       cut-off-date (:cut-off-date deal-info)
       stated-maturity-date (:stated-maturity-date deal-info)
       first-pay-date (:first-pay-date deal-info)
       first-int-date (:first-int-date deal-info)
       first-calc-date (:first-calc-date deal-info)

       pay-dates (u/gen-dates-range first-pay-date (jt/months 1) stated-maturity-date)
       int-dates (u/gen-dates-range first-int-date (jt/months 1) stated-maturity-date)
       calc-dates (map #(jt/adjust % :last-day-of-month ) (u/gen-dates-range first-calc-date (jt/months 1) stated-maturity-date))

       ;setup accounts
       principal-account (:本金帐 deal-info)
       interest-account (:收益帐 deal-info)

       ;collateral cashflows
       collection-intervals (partition 2 1 calc-dates)
       int-intervals (partition 2 1 int-dates)
       ;pool-cfs (.project-cashflow 资产池)

       ;bond cashflows
       ;cf-stmt (Table/create "Cashflow Statement" )
          ]
      )
    )
  (run-bonds [ x assump]

    )
  )





(defn run-waterfall [ wf coll-cf bond-info ]
  )


(defrecord waterfall
  [ actions ]
  )


(defrecord stmt [ date amount info ]

  )


;(defn new-account [ input-acc-list ]
;  (let [ r (atom []) ]
;    (doseq [ a (first input-acc-list) ]
;      (swap! r conj
;             (account. (get-in a [:attrs :name]) nil (Float. (get-in a [:attrs :init])) [])))
;    @r
;    ))




(defrecord account [ name type balance stmts ]
  t/Account
  (pay-bond [ x d bond ])
  (pay-fee [ x d fee ])
  (deposit [ x d amount ]
    (cons (->stmt d amount #{:deposit }) stmts)
    (+ balance amount)
    )
  )

(defn transfer-fund [ x y amt ]

  )