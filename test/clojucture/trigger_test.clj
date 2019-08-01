(ns clojucture.trigger_test
  (:require [clojure.test :refer :all]
            [clojucture.util :as u]
            [clojucture.trigger :as trg]
            [java-time :as jt]
            [clojucture.pool :as p]))



(def test-pool-cf
  (u/gen-cashflow "pool-cf-test"
                     [[:dates (u/gen-dates-ary (jt/local-date 2018 1 1) (jt/months 2) 10)]
                      [:default (double-array 10 0.01)]
                      ]

                     )

  )

(def test-pool-cf-cum (p/calc-cumulative-amount test-pool-cf "default"))

(deftest tPoolTrigger
  (let [ cum-trigger (trg/->pool-trigger "pool-t" [:pool-cumulative-default-rate > 0.03 ] false)
         t-result (trg/run-pool-trigger cum-trigger test-pool-cf-cum)
        ]
    (is (= (nth t-result 2) false))
    (is (= (nth t-result 0) false))
    (is (= (nth t-result 8) true))
    (is (= (nth t-result 9) true))
    )
  )



