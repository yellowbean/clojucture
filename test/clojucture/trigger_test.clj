(ns clojucture.trigger_test
  (:require [clojure.test :refer :all]
            [clojucture.util :as u]
            [clojucture.trigger :as trg]
            [java-time :as jt]
            [clojucture.pool :as p])
  (:import (clojucture DoubleFlow))
  )



(def test-pool-cf
  (u/gen-cashflow "pool-cf-test"
                     [[:dates (u/gen-dates-ary (jt/local-date 2018 1 1) (jt/months 2) 10)]
                      [:default (double-array 10 0.01)]
                      ]))

(def test-pool-cf2
  (u/gen-cashflow "pool-cf-test"
                     [[:dates (u/gen-dates-ary (jt/local-date 2018 1 1) (jt/months 2) 18)]
                      [:default (double-array 18 0.001)]
                      ]))

(def test-pool-cf-cum (p/calc-cumulative-amount test-pool-cf "default"))
(def test-pool-cf-cum2 (p/calc-cumulative-amount test-pool-cf2 "default"))

(deftest tPoolTrigger
  (let [ cum-trigger (trg/->pool-trigger "pool-t" {:target :pool-cumulative-default-rate :op > :threshold 0.03 :curable true} false)
        t-result (trg/run-pool-trigger cum-trigger test-pool-cf-cum)

        threshold-vector [ [ (jt/local-date 2018 1 1) (jt/local-date 2019 1 1) (jt/local-date 2020 12 1 ) ] [ 0.005 0.01 ]]
        cum-trigger-vec (trg/->pool-trigger "pool-t"
                                            {:target :pool-cumulative-default-rate :op >
                                             :threshold-vec threshold-vector :curable true } false)
        t-result-vec (trg/run-pool-trigger cum-trigger-vec test-pool-cf-cum2)

        cum-trigger-vec2 (trg/->pool-trigger "pool-uncurable"
                                            {:target :pool-cumulative-default-rate :op >
                                             :threshold-vec threshold-vector :curable false } false)

        t-result-vec2 (trg/run-pool-trigger cum-trigger-vec2 test-pool-cf-cum2)
        ]
    ;single value test
    (is (= (nth t-result 2) false))
    (is (= (nth t-result 0) false))
    (is (= (nth t-result 8) true))
    (is (= (nth t-result 9) true))


    ;vector values test
    ;(println t-result-vec)
    (is (= (nth t-result-vec 5) true))
    (is (= (nth t-result-vec 10) true))
    (is (= (nth t-result-vec 16) true))
    ;(is (= (nth t-result-vec 17) true))

    (is (= (nth t-result-vec2 5) true))
    (is (= (nth t-result-vec2 6) true))
    (is (= (nth t-result-vec2 16) true))

    )
  )



