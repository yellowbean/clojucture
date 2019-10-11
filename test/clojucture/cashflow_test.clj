(ns clojucture.cashflow-test
  (:require [clojure.test :refer :all]
            [clojucture.asset :as asset]
            [java-time :as jt]
            [clojucture.util :as u]
            [clojucture.util-cashflow :as uc])
  (:import
    [tech.tablesaw.api DoubleColumn DateColumn]
    [tech.tablesaw.columns AbstractColumn]
    [clojucture Cashflow]
    (java.security InvalidParameterException)
    (clojucture CashColumn BalanceColumn)
    )
  )

(defn sample-cf [dummy]
  (let [df (u/gen-dates-ary (jt/local-date 2018 1 1) (jt/months 1) 12)
        balf (vec (range 2200.0 -1 -200.0))
        bf (repeat 12 200.0)
        if [0.0 10.0 20.0 30.0 40.0 50.0 60.0 70.0 80.0 90.0 100.0 110.0]

        df-col (DateColumn/create "dates" df)
        balf-col (u/gen-column {:name :balance :type :balance :values balf})
        bf-col (u/gen-column {:name :principal :type :cash :values bf})
        if-col (u/gen-column {:name :interest :type :cash :values if})

        column-array (into-array AbstractColumn [df-col balf-col bf-col if-col])
        ]
    (doto (Cashflow. "sample1")
      (.addColumns column-array))))


(deftest tGrpCf
  (let [groupResult (uc/agg-cashflow-by-interval (sample-cf nil) [(jt/local-date 2018 6 1) (jt/local-date 2018 8 1)]) ]

    (println groupResult)
    )




  )

(comment
(deftest tAggCashflow
  (let [ grpCf (.groupByInterval (sample-cf nil) "N" (u/dates [(jt/local-date 2018 6 1)]) )
        aggResult (.aggByGroup (sample-cf nil) (u/dates [(jt/local-date 2018 6 1)])) ]
    ;(println grpCf)
    ;(println aggResult)
    (is (= 2 (.rowCount aggResult)))
    (is (= 1000.0 (-> (.column aggResult "Sum [principal]") (.get 0))))
    (is (= 560.0 (-> (.column aggResult "Sum [interest]") (.get 1))))
    )
  (comment

    (let [aggResult (.groupByInterval (sample-cf nil) "N" (u/dates [(jt/local-date 2018 6 1)
                                                                    (jt/local-date 2018 8 1)]))
          cf-groups (.splitByGroup aggResult)]
      (is (= (count cf-groups) 3))
      )
    (let [aggResult (.groupByInterval (sample-cf nil) "N"
                                      (u/dates [(jt/local-date 2018 6 1)
                                                (jt/local-date 2018 8 1)]))
          cf-groups (.splitByGroup aggResult)
          aggResult2 (.aggByGroup aggResult)
          ]

      (is (= (count cf-groups) 3))


      (println aggResult2)
      )
    ))
  )


