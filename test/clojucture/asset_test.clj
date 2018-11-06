(ns clojucture.asset-test
  (:require
    [java-time :as jt]
    [clojucture.asset :as asset ]
    [clojucture.assumption :as assump ]
    [clojucture.util :as u])
  (:use midje.sweet)
  )

;(def cn-rates (u/load-interest-rate "china/rates.json"))
(comment
(def test-reset-dates
  (u/gen-dates-range (jt/local-date 2017 1 1)  (jt/years 1) (jt/local-date 2020 1 1)))

(def test-loan
  (asset/->loan (jt/local-date 2018 2 1) (jt/months 1) 11 0.08 1250 :ACT_365 {} ))

(def test-loan2
  (asset/->loan (jt/local-date 2017 2 1) (jt/months 2) 11 0.12 2500 :ACT_365 {} ))

(def test-loan3
  (asset/->loan (jt/local-date 2018 1 1) (jt/months 2) 24 0.06 5000 :ACT_365 {} ))

(def test-loan-cf
  (.project-cashflow test-loan))

(def test-float-mortgage
  (asset/->float-mortgage
    (jt/local-date 2018 1 1)
    (jt/months 1)
    240
    200
    0.049
    1250000
    :ACT_365
    {:index :五年以上贷款利率 :margin 0.01 :reset-dates test-reset-dates}
    nil))

(def test-index-curve
  (assump/gen-curve :五年以上贷款利率
    [(jt/local-date 2018 1 1) (jt/local-date 2018 6 1)]
    [0.049 0.061]
  ))


(def test-float-loan
  (asset/->float-loan (jt/local-date 2018 1 1 ) (jt/months 1) 18 0.12 1500 :30_365
                      {:index :五年以上贷款利率 :margin 0.01 :reset-period 6} {:int-pay-feq 3})
  )



(fact "loan cashflow validation"
      (.get (.column test-loan-cf "dates") 0) => (jt/local-date 2018 2 1)
      (.get (.column test-loan-cf "dates") 1) => (jt/local-date 2018 3 1)
      (.get (.column test-loan-cf "dates") 11) => (jt/local-date 2019 1 1)
      (.get (.column test-loan-cf "balance") 11) => 0.0
      (.get (.column test-loan-cf "balance") 0) => 1250.0
      (.get (.column test-loan-cf "principal") 11) => 1250.0
      (.sum (.column test-loan-cf "principal") ) => 1250.0
      )

;(fact ";(.project-cashflow test-float-loan {:indexes test-index-curve})"

;      )



;(fact ";(.project-cashflow test-float-mortgage {:indexes test-index-curve})"




;      )

(def test-installment
  (asset/->installments (jt/local-date 2018 1 1 ) (jt/months 1) 12 1000 0.008 nil ))

(def test-installment-cf (.project-cashflow test-installment))


(fact "installment cashflow validation"
      (.get (.column test-installment-cf "dates") 0) => (jt/local-date 2018 1 1)
      (.get (.column test-installment-cf "dates") 1) => (jt/local-date 2018 2 1)
      (.get (.column test-installment-cf "dates") 11) => (jt/local-date 2018 12 1)
      (.get (.column test-installment-cf "balance") 11) => (roughly 83.3333 0.1)
      (.get (.column test-installment-cf "balance") 0) => 1000.0
      (.get (.column test-installment-cf "principal") 12) => (roughly 83.33333 0.1)
      (.sum (.column test-installment-cf "principal") ) => (roughly 1000.0 0.1)
      (.get (.column test-installment-cf "installment-fee") 3) => 8.0
      )

)