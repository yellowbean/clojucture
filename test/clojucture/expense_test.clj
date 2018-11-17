(ns clojucture.expense_test
  (:require [clojure.test :refer :all]
          [java-time :as jt]
          [clojucture.util :as util]
          [clojucture.expense :as exp  ])
  (:import [java.time LocalDate Period  ])

  )


(deftest test-lum-sum-fee
  (let [ t-exp (exp/->amount-expense :law-fee 1500 1500)]
    (is (= (.cal-due-amount t-exp (jt/local-date 2018 1 3)) 1500))

    (is (=
          (:balance (.receive t-exp (jt/local-date 2018 1 3) 500))
           1000))

    )
  )


(deftest test-pct-fee
  (let [ p-exp (exp/->pct-expense :trustee-fee 1000 0.001 :30_365 (jt/local-date 2018 6 1) 0)]

    )
  )
