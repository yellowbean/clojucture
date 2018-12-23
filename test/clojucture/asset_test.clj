(ns clojucture.asset-test
  (:require
    [java-time :as jt]
    [clojure.test :refer :all]
    [clojucture.asset :as asset ]
    [clojucture.assumption :as assump ]
    [clojucture.util :as u])
  )

(def test-assets (atom nil) )

(def test-reset-dates
  (u/gen-dates-range (jt/local-date 2017 1 1)  (jt/years 1) (jt/local-date 2020 1 1)))


(deftest test-loan-cf
  (let [test-loan (asset/->loan (jt/local-date 2018 2 1) (jt/months 1) 11 0.08 1250 :ACT_365 {} )
        test-loan2 (asset/->loan (jt/local-date 2017 2 1) (jt/months 2) 11 0.12 2500 :ACT_365 {} )
        test-loan3 (asset/->loan (jt/local-date 2018 1 1) (jt/months 2) 24 0.06 5000 :ACT_365 {} )
        [cf1 cf2 cf3 ] (map #(.project-cashflow %) [test-loan test-loan2 test-loan3])
    ]
    (is (= (.rowCount cf1) 12))
    (is (= (.rowCount cf2) 12))
    (is (= (.rowCount cf3) 25))

    )
  )

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


(deftest test-installment-cf
  (let [
        instl  (asset/->installments {} 30000 (jt/local-date 2018 1 1) (jt/months 1) 10 0.0027)
        instl-cf (.project-cashflow instl) ]
  (are [x y] (= x y)
       (.get (.column instl-cf "dates") 0) (jt/local-date 2018 1 1)
       (.get (.column instl-cf "dates") 1)  (jt/local-date 2018 2 1)
       (.get (.column instl-cf "dates") 10)   (jt/local-date 2018 11 1)
      )
  (are [x y] (= x y)
             (.size (.doubleColumn instl-cf "installment-fee") ) 11
             )
))


(deftest test-comm-paper-cf
  (let [cp-info { }
        cp (asset/->commercial-paper cp-info 29000 (jt/local-date 2018 3 10) (jt/local-date 2018 10 10) )
        cp-cf (.project-cashflow cp)
        ]
    (is (= (.columnCount cp-cf) 3))
    (is (= (.get (.column cp-cf "dates") 0) (jt/local-date 2018 3 10)))
    (is (= (.get (.column cp-cf "dates") 1) (jt/local-date 2018 10 10)))
    (is (= (.get (.column cp-cf "balance") 0) 29000.0))
    (is (= (.get (.column cp-cf "balance") 1) 0.0))
    (is (= (.get (.column cp-cf "principal") 0) 0.0))
    (is (= (.get (.column cp-cf "principal") 1) 29000.0))
    )
  )


(deftest test-leasing-cf
  (let [ tleasing (asset/->leasing nil (jt/local-date 2018 10 1) 24 (jt/months 3) 500)
        tleasing-cf (.project-cashflow tleasing)]
    (is (= (.rowCount tleasing-cf)) 25)
    (is (= (.get (.column tleasing-cf "dates") 0) (jt/local-date 2018 10 1)))
    (is (= (.get (.column tleasing-cf "dates") 1) (jt/local-date 2019 1 1)))
    (is (= (.get (.column tleasing-cf "rental") 0) 0.0 ))
    (is (= (.get (.column tleasing-cf "rental") 1) 500.0 ))
    (is (= (.get (.column tleasing-cf "rental") 24) 500.0))
    )
  )