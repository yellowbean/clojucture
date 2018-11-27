(ns clojucture.asset-test
  (:require
    [java-time :as jt]
    [clojure.test :refer :all]
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




)

(deftest test-installment-cf
  (let [ test-installment-info {:start-date (jt/local-date 2018 1 1) :periodicity (jt/months 1)
                               :original-balance 30000
                               :original-term 10 :period-fee-rate 0.0027
                               }
        instl  (asset/->installments test-installment-info 20000 15 )
        instl-cf (.project-cashflow instl) ]
  (are [x y] (= x y)
       (.get (.column instl-cf "dates") 0) (jt/local-date 2018 1 1)
       (.get (.column instl-cf "dates") 1)  (jt/local-date 2018 2 1)
       (.get (.column instl-cf "dates") 10)   (jt/local-date 2018 11 1)
      )
  (are [x y] (= x y)
             (.size (.doubleColumn instl-cf "installment-fee") ) 2
             )
))