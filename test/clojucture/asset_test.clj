(ns clojucture.asset-test
  (:require
    [java-time :as jt]
    [clojure.test :refer :all]
    [clojucture.helper :as h]
    [clojucture.asset :as asset]
    [clojucture.assumption :as assump]
    [clojucture.util :as u]
    [clojucture.asset :as a])
  (:import
    (clojucture RateAssumption)))



(def test-reset-dates
  (u/gen-dates-range (jt/local-date 2017 1 1) (jt/years 1) (jt/local-date 2020 1 1)))


(def test-mortgage
  (asset/->mortgage-pool
    {:start-date (jt/local-date 2014 5 5) :periodicity (jt/months 1) :term 48 :balance 20000 :period-rate 0.01}
    {:last-paid-date nil}
    20000 0.01 48 nil))

(def test-seasoned-mortgage
  (asset/->mortgage-pool
    {:start-date (jt/local-date 2014 5 5) :periodicity (jt/months 1) :term 48 :balance 20000 :period-rate 0.01}
    {:last-paid-date nil}
    12000 0.01 28 nil))

(def test-mortgage-date-rng
  (u/dates [(jt/local-date 2014 1 1) (jt/local-date 2019 1 1)]))

(deftest tm-1
  (let [
        ; 0/0 scenario
        tm-1-cf (.project-cashflow test-mortgage)
        bal-col (.column tm-1-cf "balance")
        prin-col (.column tm-1-cf "principal")
        int-col (.column tm-1-cf "interest")

        ; with 0/0 explicitly
        assump {:settle-date (jt/local-date 2015 1 1)
                :prepayment  (RateAssumption. "pc" test-mortgage-date-rng (u/ldoubles [0.0]))
                :default     (RateAssumption. "dc" test-mortgage-date-rng (u/ldoubles [0.0]))}
        tm-2-cf (.project-cashflow test-mortgage assump)
        bal-col-2 (.column tm-2-cf "balance")
        prin-col-2 (.column tm-2-cf "principal")
        int-col-2 (.column tm-2-cf "interest")
        ; with default scenario
        assump-1 {:settle-date (jt/local-date 2015 1 1)
                  :prepayment  (RateAssumption. "pc" test-mortgage-date-rng (u/ldoubles [0.0]))
                  :default     (RateAssumption. "dc" test-mortgage-date-rng (u/ldoubles [0.0]))
                  }
        tm-3-cf (.project-cashflow test-mortgage assump)
        bal-col-3 (.column tm-3-cf "balance")
        prin-col-3 (.column tm-3-cf "principal")
        int-col-3 (.column tm-3-cf "interest")
        ]
    (is (> (.get bal-col 0) 19673.323))
    (is (> (.get prin-col 4) 336.0))
    (is (< (.get int-col 4) 191.0))

    ; explicitly 0/0 should be same with default scenario
    (is (> (.get bal-col-2 0) 19673.323))
    (is (> (.get prin-col-2 4) 336.0))
    (is (< (.get int-col-2 4) 191.0))
    ))

(def test-seasoned-mortgage
  (let [ m-cf (.project-cashflow test-seasoned-mortgage nil)
        d-list (.asList (.column m-cf "dates"))
        bal-list (.asList (.column m-cf "balance"))
        prin-list (.asList (.column m-cf "principal"))
        ]
    (is (= [ (jt/local-date 2016 2 5)(jt/local-date 2018 5 5) ]   [(first d-list)(last d-list)]  ))
    (is (h/close? 373.4932 (first prin-list)))
    (is (h/close? 381.0 (nth prin-list 2)))

    (is (h/close? 11626.506 (first bal-list)))
    (is (h/close? 10868.27 (nth bal-list 2)))

    )
  )





(comment
  (deftest test-loan-cf
    (let [test-loan (asset/->loan (jt/local-date 2018 2 1) (jt/months 1) 11 0.08 1250 :ACT_365 {})
          test-loan2 (asset/->loan (jt/local-date 2017 2 1) (jt/months 2) 11 0.12 2500 :ACT_365 {})
          test-loan3 (asset/->loan (jt/local-date 2018 1 1) (jt/months 2) 24 0.06 5000 :ACT_365 {})
          [cf1 cf2 cf3] (map #(.project-cashflow %) [test-loan test-loan2 test-loan3])]

      (is (= (.rowCount cf1) 12))
      (is (= (.rowCount cf2) 12))
      (is (= (.rowCount cf3) 25))))







  (deftest test-mortgage-1
    (let [mort-info {:start-date (jt/local-date 2014 5 5) :periodicity (jt/months 1) :term 48 :balance 20000 :period-rate 0.01}
          mort (asset/->mortgage-pool mort-info nil 20000 0.01 48 nil)
          mort-cf (.project-cashflow mort)

          ; mortgage with prepayment history
          ; ppy-histroy {:prepayment [ {:date (jt/local-date 2015 5 5) :amount 3000}  ]}
          mort2-hist {:last-pmt-reset {:date (jt/local-date 2015 5 5) :balance 5856.92 :term 36 :period-rate 0.01}}
          mort2 (asset/->mortgage-pool mort-info mort2-hist 4132.55 0.01 24 nil)
          mort2-cf (.project-cashflow mort2)

          ;float mortgage
          index-curves (assump/setup-curve :LDR5Y+ [(jt/local-date 2015 10 1)] [0.049])
          mort3-float-info {:index :LDR5Y+ :spread 0.001 :reset ""}
          mort3-info {:float-info mort3-float-info :start-date (jt/local-date 2014 5 5) :periodicity (jt/months 1) :term 60 :balance 20000 :period-rate 0.01}
          mort3 (asset/->mortgage-pool mort3-info nil 30000 0.02 20 nil)]


      (is (= (.rowCount mort-cf) 49))
      ;(println mort-cf)
      (is (< (Math/abs (- (.get (.column mort-cf "balance") 1) 19673.32)) 0.01))

      ;(println mort2-cf)

      (is (= (.rowCount mort2-cf) 25))
      ;(println mort-cf)
      (is (< (Math/abs (- (.get (.column mort2-cf "balance") 3) 3668.32)) 0.01))))

  )

(deftest test-mortgage-assump
  (let [mort-info {:start-date (jt/local-date 2014 5 5) :periodicity (jt/months 1) :term 48 :balance 20000 :period-rate 0.01}
        mort (asset/->mortgage-pool mort-info nil 20000 0.01 48 nil)
        assump-info {:prepay-rate [] :default-rate [] :recovery-lag 10 :recovery-rate 0.4}]))







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
  (assump/setup-curve :五年以上贷款利率
                      [(jt/local-date 2018 1 1) (jt/local-date 2018 6 1)]
                      [0.049 0.061]))


(comment
  (def test-float-loan
    (asset/->float-loan (jt/local-date 2018 1 1) (jt/months 1) 18 0.12 1500 :30_365
                        {:index :五年以上贷款利率 :margin 0.01 :reset-period 6} {:int-pay-feq 3}))

  )

(deftest test-installment-cf
  (let [
        instl (asset/->installments 30000 (jt/local-date 2018 1 1) (jt/months 1) 10 0.0027 {})
        instl-cf (.project-cashflow instl)]
    (are [x y] (= x y)
               (.get (.column instl-cf "dates") 0) (jt/local-date 2018 1 1)
               (.get (.column instl-cf "dates") 1) (jt/local-date 2018 2 1)
               (.get (.column instl-cf "dates") 10) (jt/local-date 2018 11 1))

    (are [x y] (= x y)
               (.size (.doubleColumn instl-cf "installment-fee")) 11)))




(deftest test-comm-paper-cf
  (let [cp-info {}
        cp (asset/->commercial-paper 29000 (jt/local-date 2018 3 10) (jt/local-date 2018 10 10) cp-info)
        cp-cf (.project-cashflow cp)]

    (is (= (.columnCount cp-cf) 3))
    (is (= (.get (.column cp-cf "dates") 0) (jt/local-date 2018 3 10)))
    (is (= (.get (.column cp-cf "dates") 1) (jt/local-date 2018 10 10)))
    (is (= (.get (.column cp-cf "balance") 0) 29000.0))
    (is (= (.get (.column cp-cf "balance") 1) 0.0))
    (is (= (.get (.column cp-cf "principal") 0) 0.0))
    (is (= (.get (.column cp-cf "principal") 1) 29000.0))))




(deftest test-leasing-cf
  (let [tleasing (asset/->leasing (jt/local-date 2018 10 1) 24 (jt/months 3) 500 nil)
        tleasing-cf (.project-cashflow tleasing)
        tleasing-with-deposit (asset/->leasing (jt/local-date 2018 10 1) 24 (jt/months 1) 400 {:deposit-balance 100})
        tleasing-with-deposit-cf (.project-cashflow tleasing-with-deposit)

        tleasing-def-cf (.project-cashflow tleasing {:default-term 5})
        ]

    (is (= (.rowCount tleasing-cf)) 25)
    (is (= (.get (.column tleasing-cf "dates") 0) (jt/local-date 2018 10 1)))
    (is (= (.get (.column tleasing-cf "dates") 1) (jt/local-date 2019 1 1)))
    (is (= (.get (.column tleasing-cf "rental") 0) 0.0))
    (is (= (.get (.column tleasing-cf "rental") 1) 500.0))
    (is (= (.get (.column tleasing-cf "rental") 24) 500.0))

    ;leasing with deposit
    (is (= (.rowCount tleasing-with-deposit-cf)) 25)
    (is (= (.get (.column tleasing-with-deposit-cf "dates") 0) (jt/local-date 2018 10 1)))
    (is (= (.get (.column tleasing-with-deposit-cf "dates") 1) (jt/local-date 2018 11 1)))
    (is (= (.get (.column tleasing-with-deposit-cf "rental") 0) 0.0))
    (is (= (.get (.column tleasing-with-deposit-cf "rental") 1) 400.0))
    (is (= (.get (.column tleasing-with-deposit-cf "rental") 24) 400.0))
    (is (= (.get (.column tleasing-with-deposit-cf "deposit") 0) 100.0))
    (is (= (.get (.column tleasing-with-deposit-cf "deposit") 24) -100.0))

    (is (= (.rowCount tleasing-def-cf)) 6)
    ))



;; test cf with assumption
(deftest loan-with-assump
  (let [info {:start-date    (jt/local-date 2018 1 1)
              :first-pay     (jt/local-date 2018 3 1) :periodicity (jt/months 3)
              :term          36 :rate 0.08 :balance 1000
              :maturity-date (jt/local-date 2026 12 1)}
        assump-ppy (assump/gen-pool-assump-df :cpr [0.00] [(jt/local-date 2014 1 1) (jt/local-date 2027 1 1)])
        assump-def (assump/gen-pool-assump-df :cdr [0.00] [(jt/local-date 2014 1 1) (jt/local-date 2027 1 1)])
        assump {:prepayment assump-ppy :default assump-def}
        tloan (asset/->loan info 1000 24 0.08 nil)]
    ;(println "Cashflow: " (.project-cashflow tloan assump) )

    ))




(deftest tUtilFunc
  (let [pmt-1 (a/cal-period-pmt 2250000000 240 (/ 0.059 12))]
    (is (< (Math/abs (- pmt-1 15990164.72)) 0.01)

        )
    ))
  
