(ns clojucture.cashflow-test
  (:require [clojure.test :refer :all]
            [clojucture.asset :as asset]
            [java-time :as jt]
            [clojucture.util :as u])
  (:import
    [tech.tablesaw.api DoubleColumn DateColumn]
    [tech.tablesaw.columns AbstractColumn]
    [clojucture Cashflow]
    )
  )

(defn sample-cf [dummy]
  (let [df (u/gen-dates-ary (jt/local-date 2018 1 1) (jt/months 1) 12)
        balf (double-array (range 2200 -1 -200))
        bf (double-array 12 200)
        if (double-array [0 10 20 30 40 50 60 70 80 90 100 110])

        df-col (DateColumn/create "dates" df)
        balf-col (DoubleColumn/create "balance" balf)
        bf-col (DoubleColumn/create "principal" bf)
        if-col (DoubleColumn/create "interest" if)

        column-array (into-array AbstractColumn [df-col balf-col bf-col if-col])
        ]
    (doto (Cashflow. "sample1")
      (.addColumns column-array))))


(deftest tAggCashflow
  (let [aggResult (.aggregateByInterval (sample-cf nil) "N"
                                        (u/dates [;(jt/local-date 2018 1 1)
                                                  (jt/local-date 2018 6 1)]))]
    (is (= (count aggResult) 2))



    (let [aggResult (.aggregateByInterval (sample-cf nil) "N"
                                          (u/dates [;(jt/local-date 2018 1 1)
                                                    (jt/local-date 2018 6 1)
                                                    (jt/local-date 2018 8 1)]))]
      (is (= (count aggResult) 3))

      ))
  )
      



