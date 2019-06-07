(ns clojucture.util-cashflow-test
  (:require [clojure.test :refer :all]
            [clojucture.util :as u]
            [clojucture.util-cashflow :as cfu ]
            [java-time :as jt]
            )
  (:import [java.time LocalDate]))




(deftest tD
  (let [ m1 {:first-date (jt/local-date 2019 1 1 ) :interval (jt/months 1) :times 4}
        d1 (cfu/gen-dates m1)
        ]
    (is (=  (first d1) (jt/local-date 2019 1 1)))
    (is (=  (second d1) (jt/local-date 2019 2 1)))
    (is (=  (last d1) (jt/local-date 2019 4 1)))

    )

  )

(deftest tCol
  (let [ m1 {:name "double-col" :type :double :values [1 2 3 4]}
        c1 (cfu/gen-column m1)]
    (is (= (.get c1 0) 1.0))
    (is (= (.get c1 1) 2.0))
    (is (= (.get c1 2) 3.0))
    (is (= (.get c1 3) 4.0))
    )

  )

(deftest tCashflow
  (let [ m1 {:name  "cashflow1"
             :dates {:first-date (jt/local-date 2020 3 3) :interval (jt/months 3) :times 3} }
        cf1 (cfu/gen-cashflow m1)
    ]
    (is (= (.get (.column cf1 0) 2) (jt/local-date 2020 9 3) ))
  ))

(comment

(deftest tCashflowJoinDates
  (let[ testCF (u/gen-cashflow "cashflow to be join"
                  []

                   )
        obs-dates (-> (u/gen-dates (jt/local-date 2019 1 1) (jt/months 2) 4) (u/dates))
        resultCF (.insertDates testCF obs-dates)
       ]



    )

  ))
