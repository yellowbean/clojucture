(ns clojucture.reader.base-test
  (:require [clojure.test :refer :all]
            [clojucture.reader.base :as base]
            [java-time :as jt]))



(deftest t-parsing-dates
  (comment
    (let [a (base/parsing-dates "2018-01-23")]
      (is (= a (jt/local-date 2018 1 23))))


    (let [[a b] (base/parsing-dates "2019-01-01,2011-01-01")]
      (is (= (jt/local-date 2019 1 1) a))
      (is (= (jt/local-date 2011 1 1) b)))

    (let [x (base/parsing-dates "2013-03-20Q,2014-06-30")]

      (is (= (first x) (jt/local-date 2013 3 20)))
      (is (= (second x) (jt/local-date 2013 6 20)))
      (is (= (last x) (jt/local-date 2014 6 30)))
      )
    )
  (let [ x (base/parsing-dates "2013-02-01,2013-03-20Q,2014-06-30")]
    (is (= (first x)  (jt/local-date 2013 2 1)))
    (is (= (second x)  (jt/local-date 2013 3 20)))
    (is (= (nth x 2)  (jt/local-date 2013 6 20)))
    ;(is (.isEqual (last x) (jt/local-date 2014 6 30)))
    ;(print (type (last x) ))
    ;(is (= (last x) (jt/local-date 2014 6 30)))
    )
  (let [x (base/parsing-dates "2013-03-20M,2014-06-20")]
    (is (= (first x)  (jt/local-date 2013 3 20)))
    (is (= (second x)  (jt/local-date 2013 4 20)))
    (is (= (nth x 2)  (jt/local-date 2013 5 20)))
    (is (= (last x) (jt/local-date 2014 6 20)))

    )
    )
