(ns clojucture.pool-test
  (:require
    [java-time :as jt]
    [clojure.test :refer :all]
    [clojucture.asset :as asset ]
    [clojucture.asset-test :as at]
    [clojucture.pool :as p]
    [clojucture.util :as u]
    )
  )

(def date-intervals
  (u/gen-dates-interval [(jt/local-date 2016 4 1) (jt/local-date 2018 4 1) (jt/local-date 2023 1 2)]))


(deftest test-pool-cf
         (let [tp (p/->pool [ ])] )

         )
