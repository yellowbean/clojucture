(ns clojucture.pool-test
  (:require
    [java-time :as jt]
    [clojure.test :refer :all]
    [clojucture.asset :as asset ]
    [clojucture.pool :as p]
    [clojucture.util :as u]
    )
  )
(comment
  (def date-intervals
    (u/gen-dates-interval
      [(jt/local-date 2016 4 1) (jt/local-date 2018 4 1) (jt/local-date 2023 1 2)]))


  (deftest test-pool-cf
    (let [lm1 {:info {:start-date (jt/local-date 2018 3 1)} }

          l-list (asset/map->loan lm1)
          tp (p/->pool [])

          ])

    )
  )
