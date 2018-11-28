(ns clojucture.expense_test
  (:require [clojure.test :refer :all]
          [java-time :as jt]
          [clojucture.util :as util]
          [clojucture.account :as acc]
          [clojucture.expense :as exp  ])
  (:import [java.time LocalDate Period  ])

  )


(deftest test-lum-sum-fee
  (let [ acc-info {:name :law-fee }
         t-exp (exp/->amount-expense acc-info 1500 []  0)
         cash-acc (acc/->account :cash :cash 2000 [])
        ]
    (is (= (.cal-due-amount t-exp (jt/local-date 2018 1 3)) 1500))

    (let [ [new-exp new-acc]  (.receive t-exp (jt/local-date 2018 1 3) cash-acc) ]
      (is (= (:balance new-acc) 500))
      (is (= (:balance new-exp) 0))
    )
    )
  )


(deftest test-pct-fee
  (let [cash-acc (acc/->account :cash :cash 2000 [])

        p-exp-info {:name :trustee-fee :pct 0.001 :day-count :30_365 :type :yearly}
        p-exp (exp/->pct-expense p-exp-info [] (jt/local-date 2018 6 1) 0)
        due-1 (.cal-due-amount p-exp (jt/local-date 2018 12 1) 50000)
        [new-p-exp new-acc] (.receive p-exp (jt/local-date 2018 12 1) 50000 cash-acc)
        new-stmt (:stmts new-p-exp)


        p-exp-info-2 {:name :VAT :pct 0.03 :day-count :30_365 :type :one-off}
        p-exp-2 (exp/->pct-expense p-exp-info-2 [] (jt/local-date 2018 6 1) 0)
        due-2 (.cal-due-amount p-exp-2 (jt/local-date 2018 12 1) 10000)
        [new-p-exp new-acc-2] (.receive p-exp-2 (jt/local-date 2018 12 1) 10000 new-acc)

        ]
    (is (= due-1 25.0))
    (is (= (:balance new-acc) (- 2000 25.0) ))

    (is (= due-2 300.0))
    (is (= (:balance new-acc-2) (- 2000 25.0 300.0) ))

    )
  )


