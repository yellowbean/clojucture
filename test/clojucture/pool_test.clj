(comment
(ns clojucture.pool-test
  (:require
    [java-time :as jt]
    [clojucture.asset :as asset ]
    [clojucture.asset-test :as at]
    [clojucture.pool :as p]
    [clojucture.util :as u]
    )
  (:use midje.sweet)
  )

(def date-intervals (u/gen-dates-interval [(jt/local-date 2016 4 1) (jt/local-date 2018 4 1) (jt/local-date 2023 1 2)]))

(def test-pool
  (p/->pool
    [ at/test-loan at/test-loan2 at/test-loan3]
    date-intervals
    )
)

)
