(ns clojucture.java-util
  (:require [clojure.test :refer :all]
            [clojucture.util :as util]
            [java-time :as jt]
            [clojucture.util :as u])
  (:import clojucture.DoubleFlow
           clojucture.RateAssumption
           [java.time LocalDate])
  )


; test on double flow
(deftest tDoubleFlow
  (let [d (util/gen-dates-ary (jt/local-date 2018 1 3) (jt/months 1) 3)
        v (double-array [1 2 3])
        al (into-array LocalDate [(jt/local-date 2018 2 2) (jt/local-date 2018 2 2) (jt/local-date 2018 3 15)])
        tdf (DoubleFlow. "tDF" d v)
        aldf (.align tdf "tDF_Algin" al)
        al2 (into-array LocalDate [(jt/local-date 2018 2 2) (jt/local-date 2018 2 15) (jt/local-date 2018 3 15) (jt/local-date 2018 4 15)])
        aldf2 (.align tdf "tDF_Algin" al2)
        ]

    (is (= (.columnCount aldf) 2))
    (is (= (.get (.column aldf 1) 0) 1.0))
    (is (= (.get (.column aldf 1) 1) 1.0))
    (is (= (.get (.column aldf 1) 2) 3.0))

    (is (= (.get (.column aldf2 1) 0) 1.0))
    (is (= (.get (.column aldf2 1) 1) 2.0))
    (is (= (.get (.column aldf2 1) 2) 3.0))
    (is (= (.get (.column aldf2 1) 3) 3.0))

    ))

(deftest tRateAssumption
  (let [ dr (into-array LocalDate [ (jt/local-date 2017 1 1) (jt/local-date 2020 1 1)])
        dv (into-array Double [0.01])
        ta (RateAssumption. "Flat Rate" dr dv)
        project-result (.project ta (into-array LocalDate [(jt/local-date 2018 11 1) (jt/local-date 2019 5 1 )]))
        apply-result (.apply ta (into-array LocalDate [(jt/local-date 2018 11 1) (jt/local-date 2019 5 1 )]))
        ]
    (= (first apply-result) 0.01)
    ;(println apply-result2)

    )
  )

(comment
(deftest tDoubleFlowApply
  (let [ da (->>
              (u/gen-dates-range (jt/local-date 2018 1 1) (jt/months 1) (jt/local-date 2019 1 1))
              (into-array LocalDate))
         ra (double-array (range 0.01 0.12 0.01))
         rassump (RateAssumption. "Rate" da ra)
         observe-dates (into-array LocalDate [(jt/local-date 2018 5 3) (jt/local-date 2018 10 1)])

         applied-rates (.apply rassump observe-dates)
        ]
    (println applied-rates)

    )
  ))

