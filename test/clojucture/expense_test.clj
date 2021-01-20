(ns clojucture.expense_test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojucture.builder :as br]
            [java-time :as jt]
            [clojucture.account :as acc]
            [clojucture.expense :as exp]
            [clojucture.local.nowhere :as nw]
            )
  )

(def test-deal
  (-> (edn/read-string (slurp (io/resource "basic02.txt")))
      (br/load-deal)))


(deftest test-exp-simple-balance
  (let [simple-exp (exp/setup-expense
                     {:name "simple-balance-exp" :balance 500 :last-paid-date (jt/local-date 2020 10 10)})
        due-exp-1 (exp/cal-due-expense nil simple-exp (jt/local-date 2020 10 10))
        due-exp-2 (exp/cal-due-expense nil simple-exp (jt/local-date 2020 10 11))
        ]
    (is (zero? (- due-exp-1 500)))
    (is (zero? (- due-exp-2 500)))
    ))


(deftest test-exp-annual-pct
  (let [annual-pct-exp (exp/setup-expense {:name           "trustee fee"
                                           :info           {:base [:projection :pool :sum-current-balance] :annual-pct 0.001 :day-count :30_365}
                                           :last-paid-date (jt/local-date 2020 11 13) :arrears 0}
                                          )

        ;run-d (nw/run-deal test-deal nil)
        ;due-exp (exp/cal-due-expense  run-d annual-pct-exp (jt/local-date 2021 01 10))
        ]
    ;(prn due-exp)

    )
  )


(comment

  (deftest test-lum-sum-fee
    (let [acc-info {:name :law-fee}
          t-exp (exp/->amount-expense acc-info [] nil 1500)
          cash-acc (acc/->account :cash :cash 2000 [])
          ]
      (is (= (.cal-due-amount t-exp (jt/local-date 2018 1 3)) 1500))

      (let [[new-acc new-exp] (exp/pay-expense (jt/local-date 2018 1 3) cash-acc t-exp)]
        (is (= (:balance new-acc) 500))
        (is (= (:balance new-exp) 0))
        (is (= (:amount (first (:stmt new-exp))) 1500)))
      )
    )


  (deftest test-pct-fee
    (let [cash-acc (acc/->account :cash :cash 2000 [])

          p-exp-info {:name :trustee-fee :pct 0.001 :day-count :30_365}
          p-exp (exp/->pct-expense-by-amount p-exp-info [] (jt/local-date 2018 6 1) 0)
          due-1 (.cal-due-amount p-exp (jt/local-date 2018 12 1) 50000)
          [new-acc new-p-exp] (exp/pay-expense-at-base (jt/local-date 2018 12 1) cash-acc p-exp 50000)
          new-stmt (:stmts new-p-exp)


          p-exp-info-2 {:name :VAT :pct 0.03 :day-count :30_365}
          p-exp-2 (exp/->pct-expense-by-rate p-exp-info-2 [] (jt/local-date 2018 6 1) 0)
          due-2 (.cal-due-amount p-exp-2 (jt/local-date 2018 12 1) 10000)
          [new-acc-2 new-p-exp] (exp/pay-expense-at-base (jt/local-date 2018 12 1) new-acc p-exp-2 10000)

          ]
      (is (= due-1 25.0))
      (is (= (:balance new-acc) (- 2000 25.0)))

      (is (= due-2 300.0))
      (is (= (:balance new-acc-2) (- 2000 25.0 300.0)))

      )
    )


  (deftest test-recu-fee
    (let [cash-acc (acc/->account :cash :cash 2000 [])
          r-exp (exp/->recur-expense
                  {:start-date (jt/local-date 2018 1 1) :period (jt/years 1) :end-date (jt/local-date 2020 1 1)
                   :amount     100} [] 0)
          ]
      (is (= (.cal-due-amount r-exp (jt/local-date 2018 1 1)) 100))
      (is (= (.cal-due-amount r-exp (jt/local-date 2019 1 1)) 100))
      (is (= (.cal-due-amount r-exp (jt/local-date 2020 1 1)) 100))

      )

    )

  )