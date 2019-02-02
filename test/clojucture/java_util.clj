(ns clojucture.java-util
  (:require [clojure.test :refer :all]
            [clojucture.util :as util]
            [java-time :as jt])
  (:import clojucture.DoubleFlow
           [java.time LocalDate])
  )


; test on double flow
(deftest tDoubleFlow
  (let [ d  (util/gen-dates-ary (jt/local-date 2018 1 3) (jt/months 1) 3)
         v  (double-array [ 1 2 3 ])
         al (into-array LocalDate [(jt/local-date 2018 2 2) (jt/local-date 2018 2 2) (jt/local-date 2018 3 15)])
         tdf  (DoubleFlow. "tDF" d v)
         aldf (.align tdf "tDF_Algin" al)
        ]
    (is (= (.columnCount aldf) 2))
    (is (= (.get (.column aldf 1) 0)  1.0))
    (is (= (.get (.column aldf 1) 1)  1.0))
    (is (= (.get (.column aldf 1) 2)  3.0))



    ;(is (= (first aldf_c) 1))
    )
  )