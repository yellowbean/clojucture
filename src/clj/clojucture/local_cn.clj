
(ns clojucture.local_cn
  (:require
    [clojucture.asset :as asset]
    [clojucture.account :as acc]
    [clojucture.deal :as d]
    [clojucture.bond :as b]
    [clojucture.pool :as p]
    [clojucture.util :as u]
    [java-time :as jt])
  (:import
    [tech.tablesaw.api Table ColumnType]
    )
  )


(def test-info {
  :accounts [
             :混同储备账户 :税收储备账户 :流动性储备账户 :抵销储备账户 :服务转移和通知储备账户
             :收入分账户 :本金分账户
             ]
  :dates    {:初始起算日 "2018-10-1" :第一计息日 nil :第一支付日 nil :支付日 nil :信托分配日 "收款期间最后1日+第10个工作日"
             :计息日   "每月26日" :法定到期日 nil :预期到期日 nil
             :计算日   "2019-1-31,每月底"}
  :pools    {}
  :bonds    {}
  :triggers {

             }
  :expense  {}

  }



(defrecord china-bank-deal [ deal-info opt status ]
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

          ;triggers
          triggers  (:triggers deal-info)


          ;collateral cashflows
          collection-intervals (partition 2 1 calc-dates)
          int-intervals (partition 2 1 int-dates)
          ;pool-cfs (.project-cashflow 资产池)

          ;bond cashflows
          ;cf-stmt (Table/create "Cashflow Statement" )
          ]
      )
    )
  (run-triggers [ x ]


    )

  (run-bonds [ x assump]

    )
  )

(comment

  (defrecord exchange-deal
    [ info pool bond triggers waterfall opt ]
    d/Deal
    (run-assets [ x ]
      (let [collect-intervals (into-array ^LocalDate (d/gen-pool-collect-interval info))
            pool-cf (.collect-cashflow pool collect-intervals)]
        pool-cf )
      )

    (run-bonds  [ x ]

      )

    (run-deal [ x ]
      (let [ pool-cf (run-assets x)
            projections nil
            ]
        )
      )
    )

  (defrecord pass-through-deal
    [ info pool waterfall bond opt ]
    d/Deal
    (run-assets [ x ]
      (let [collect-intervals (into-array ^LocalDate (d/gen-pool-collect-interval info))
            pool-cf (.collect-cashflow pool collect-intervals)]
        pool-cf )
      )
    (run-bonds [ x ]
      (let [ pool-cf   (.run-assets x)
            {:分配日 dist-dates }  info ]
        )
      )
    )

  )