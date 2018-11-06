(ns clojucture.util_test
  (:require
    [clojure.test :refer :all]
    [clojucture.util :as u]
    [java-time :as jt])
  (:use midje.sweet)
  )


(def test-curve [ [(jt/local-date 2018 1 1) 0.04] [(jt/local-date 2018 6 1) 0.05] ])
(def test-dates-ary (u/gen-dates-ary (jt/local-date 2018 1 1) (jt/months 1) 12 ))

(vec (u/gen-float-period-rates test-dates-ary test-curve))


; date generation
(deftest date-vector
  (let [ date-vector-1 (u/gen-dates (jt/local-date 2018 1 1) (jt/months 1) 20)
         date-vector-2 (u/gen-dates (jt/local-date 2018 1 1) (jt/years 1) 3)

         date-vector-3 (u/gen-dates (jt/local-date 2018 1 1) (jt/months 2) 5)
        ]
    (is (= (first date-vector-1) (jt/local-date 2018 1 1)))
    (is (= (last date-vector-1) (jt/local-date 2019 8 1)))
    (is (= (count date-vector-1) 20))

    (is (= (second date-vector-2) (jt/local-date 2019 1 1)))

    (is (= (second date-vector-3) (jt/local-date 2018 3 1)))
    )
  )



; column generation
(comment
(deftest create-d-column
  (let [ test-d-array (double-array [3 4 5])
         test-d-array-size (alength test-d-array)
         d-col (u/gen-column [:balance test-d-array])]
    (is (= test-d-array-size (.size d-col)))
    )
  )
)
; table generation
(deftest create-table



  )
