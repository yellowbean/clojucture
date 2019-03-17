(ns clojucture.reader.base-test
  (:require [clojure.test :refer :all]
            [clojucture.reader.base :as base]
            [java-time :as jt]))



(deftest t-parsing-dates
  (let [ a (base/parsing-dates "2018-01-23")]
    (is (= a (jt/local-date 2018 1 23))))


  (let [ [ a b ]  (base/parsing-dates "2019-01-01,2011-01-01")]
    (is (= (jt/local-date 2019 1 1) a))
    (is (= (jt/local-date 2011 1 1) b)) )


  )
