(ns clojucture.cashflow-test
  (:require [clojure.test :refer :all]
            [clojucture.asset :as asset]
            [java-time :as jt]
            [clojucture.util :as u]
            )
)
(def sample-cf
  (let [ df (u/gen-dates-ary (jt/local-date 2018 1 1) (jt/months 1) 12)
        bf (double-array 12 200)
        if (double-array [0 10 20 30 40 50 60 70 80 90 100 110]) ]
  (u/gen-cashflow "sample1"
    [ [:dates df ]
      [:principal bf]
      [:interest if]
     ] )
))


