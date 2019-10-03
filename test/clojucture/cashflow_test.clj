(ns clojucture.cashflow-test
  (:require [clojure.test :refer :all]
            [clojucture.asset :as asset]
            [java-time :as jt]
            [clojucture.util :as u])
  (:import
    [tech.tablesaw.api DoubleColumn DateColumn]
    [tech.tablesaw.columns AbstractColumn]
    [clojucture Cashflow]
    (java.security InvalidParameterException))
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


(deftest tGrpCf

  (let [groupResult (.groupByInterval (sample-cf nil) "N" (u/dates [(jt/local-date 2018 6 1) (jt/local-date 2018 8 1)]))
        grp-idx-list (-> (.column groupResult "Group Index") (.asList) (seq))]

    ;(println groupResult)
    (is (= 0 (first grp-idx-list)))
    (is (= 1 (nth grp-idx-list 5)))
    (is (= 1 (nth grp-idx-list 6)))
    (is (= 2 (nth grp-idx-list 7)))
    (is (= 2 (nth grp-idx-list 8)))
    (is (= 2 (last grp-idx-list)))
    )

  (let [groupResult (.groupByInterval (sample-cf nil) "N" (u/dates [(jt/local-date 2018 6 1)]))
        grp-idx-list (-> (.column groupResult "Group Index") (.asList) (seq))]

    ;(println groupResult)
    (is (= 0 (first grp-idx-list)))
    (is (= 0 (nth grp-idx-list 4)))
    (is (= 1 (nth grp-idx-list 5)))
    (is (= 1 (nth grp-idx-list 7)))
    (is (= 1 (last grp-idx-list)))
    )

  (let []
    (is (thrown? InvalidParameterException (.groupByInterval (sample-cf nil) "N" (u/dates [(jt/local-date 2017 9 1)]))))
    (is (thrown? InvalidParameterException (.groupByInterval (sample-cf nil) "N" (u/dates [(jt/local-date 2017 9 1) (jt/local-date 2018 6 1)]))))
    (is (thrown? InvalidParameterException (.groupByInterval (sample-cf nil) "N" (u/dates [(jt/local-date 2019 9 1)]))))
    (is (thrown? InvalidParameterException (.groupByInterval (sample-cf nil) "N" (u/dates [(jt/local-date 2018 9 1) (jt/local-date 2019 9 1)]))))
    )

  )

(comment

  (deftest tAggCashflow
    (let [aggResult (.groupByInterval (sample-cf nil) "N" (u/dates [(jt/local-date 2018 6 1)]))

          cf-groups (.splitByGroup aggResult)
          ]

      (is (= (count cf-groups) 2))
      )

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

    )
  )


