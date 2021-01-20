(ns clojucture.util-cashflow-test
  (:require [clojure.test :refer :all]
            [clojucture.util :as u]
            [clojucture.io.csv :as io-csv]
            [clojucture.io.html :as io-html]
            [clojucture.util-cashflow :as cfu]
            [java-time :as jt]
            )
  (:import [tech.tablesaw.aggregate AggregateFunctions]))




(deftest tD
  (let [m1 {:first-date (jt/local-date 2019 1 1) :interval (jt/months 1) :times 4}
        d1 (cfu/gen-dates m1)
        ]
    (is (= (first d1) (jt/local-date 2019 1 1)))
    (is (= (second d1) (jt/local-date 2019 2 1)))
    (is (= (last d1) (jt/local-date 2019 4 1)))

    )

  )

(deftest tCol
  (let [m1 {:name "double-col" :type :double :values [1 2 3 4]}
        c1 (u/gen-column m1)]
    (is (= (.get c1 0) 1.0))
    (is (= (.get c1 1) 2.0))
    (is (= (.get c1 2) 3.0))
    (is (= (.get c1 3) 4.0))
    )

  )

(deftest tTs
  (let [ts (cfu/gen-ts
             {:name   ""
              :dates  {:first-date (jt/local-date 2019 6 1) :interval (jt/months 3) :times 4}
              :values {:type :double :values [1.0 2.0 3.0 4.0] :name "PRINCIPAL"}})]
    (is (= (.get (.column ts 1) 1) 2.0))))


(deftest tCashflow
  (let [m1 {:name  "cashflow1"
            :dates {:first-date (jt/local-date 2020 3 3) :interval (jt/months 3) :times 3}}
        cf1 (cfu/gen-cashflow m1)
        m2 (assoc m1 :init-bal 1000)
        cf2 (cfu/gen-cashflow m2)
        ;m2 (assoc m1 :cols [ {:type :double :name "principal" :values [ 10 20 30] } ] )
        ]
    (is (= (.get (.column cf1 0) 2) (jt/local-date 2020 9 3)))

    ;cf2
    ;(println cf2)
    (is (= (.get (.column cf2 1) 1) 1000.0))

    ))

(deftest tFindByDate
  (let [cf1 (cfu/gen-ts
              {:name   ""
               :dates  {:first-date (jt/local-date 2019 6 1) :interval (jt/months 3) :times 4}
               :values {:type :double :values [1.0 2.0 3.0 4.0] :name "PRINCIPAL"}})
        r (cfu/find-row-by-date cf1 (jt/local-date 2019 9 1))
        ]
    (is (= (.getDate r "DATES") (jt/local-date 2019 9 1)))
    (is (= (.getDouble r "PRINCIPAL") 2.0))

    ))

(deftest tTrancateEmptyRows
  (let [t-cf (io-csv/read-cf "pool_cfs_ending_empty.csv" [:date :double :double])]
    (is (= 13 (.rowCount t-cf)))
    (is (= 11 (.rowCount (cfu/drop-rows-if-empty t-cf))))
    )
  (let [t-cf (io-csv/read-cf "pool_cfs_ending_empty1.csv" [:date :double :double])]
    (is (= 13 (.rowCount t-cf)))
    (is (= 12 (.rowCount (cfu/drop-rows-if-empty t-cf))))
    )
  (let [t-cf (io-csv/read-cf "pool_cfs_ending_empty2.csv" [:date :double :double])]
    (is (= 13 (.rowCount t-cf)))
    (is (= 12 (.rowCount (cfu/drop-rows-if-empty t-cf))))
    )
  )


(deftest tSubTableByDates
  (let [t-cf (io-csv/read-cf "pool_cfs_ending_empty.csv" [:date :double :double])
        after-may-2 (cfu/sub-cashflow t-cf :> (jt/local-date 2019 5 2))
        after-may-1 (cfu/sub-cashflow t-cf :> (jt/local-date 2019 5 1))
        after-on-may-1 (cfu/sub-cashflow t-cf :>= (jt/local-date 2019 5 1))]
    (is (= (dec (.rowCount after-on-may-1)) (.rowCount after-may-1)))
    (is (= (.rowCount after-may-2) (.rowCount after-may-1)))

    )
  )


(deftest tAddBal

  (let [t-cf (io-csv/read-cf "pool_cfs_add_bal.csv" [:date :double :double])
        t-cf-with-bal (cfu/add-end-bal-column t-cf 2575)    ; add with end balance
        end-bal-col (.column t-cf-with-bal "end-balance")

        t-cf-with-beg-bal (cfu/add-beg-bal-column t-cf 2575) ; add with end balance
        beg-bal-col (.column t-cf-with-beg-bal "begin-balance")

        ]
    (is (= (.get end-bal-col 0) 2275.0))
    (is (= (.get end-bal-col 1) 1990.0))
    (is (= (.get end-bal-col 10) 100.0))
    (is (= (.get end-bal-col 11) 0.0))


    (is (= (.get beg-bal-col 0) 2575.0))
    (is (= (.get beg-bal-col 1) 2275.0))
    (is (= (.get beg-bal-col 11) 100.0))
    (is (= (.get beg-bal-col 12) 0.0))
    ))


(deftest tAggCfByInterval
  (let [t-cf (io-csv/read-cf "pool_cfs_agg.csv" [:date :double :double])
        agg-cf (cfu/agg-cashflow-by-interval t-cf [ (jt/local-date 2019 4 1) (jt/local-date 2019 8 1)]   )
        prin-col (.column agg-cf "principal")
        int-col (.column agg-cf "interest") ]

    (is (= 855.0 (.get prin-col 0)))
    (is (= 930.0 (.get prin-col 1)))
    (is (= 690.0 (.get prin-col 2)))

    (is (= 66.0 (.get int-col 0)))
    (is (= 20.0 (.get int-col 1)))
    (is (= 20.0 (.get int-col 2)))


    )
  )


(deftest tSumByDate

  (let [t-cf (io-csv/read-cf "pool_cfs_agg_by_date.csv" [:date :double :double])
        agg-cf (cfu/sum-cashflow-by-date t-cf "principal" "dates")
        d-col (.column agg-cf "dates")
        p-col (.column agg-cf 1)
        ]
    (is (= (.get d-col 0) (jt/local-date 2019 1 1)))
    (is (= (.get d-col 1) (jt/local-date 2019 3 1)))


    (is (= (.get p-col 0) 585.0))
    (is (= (.get p-col 1) 525.0))

    )
  )
(comment

  (deftest tCashflowJoinDates
    (let [testCF (u/gen-cashflow "cashflow to be join"
                                 []

                                 )
          obs-dates (-> (u/gen-dates (jt/local-date 2019 1 1) (jt/months 2) 4) (u/dates))
          resultCF (.insertDates testCF obs-dates)
          ]

      )

    ))
