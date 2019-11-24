(ns clojucture.spv_test
  (:require
    [clojure.test :refer :all]
    [clojucture.spv :as spv]
    [clojucture.assumption :as assump]
    [clojucture.local.china :as cn]
    [clojucture.local.china-test :as cn-t]
    [java-time :as jt]))








(deftest query-deal
  (let [jy-bank (cn/load-deal cn-t/jy-info "2018-05-26")
        assmp (assump/build {:p {:name   :prepayment :type :cpr
                                 :dates  [(jt/local-date 2017 1 1) (jt/local-date 2049 1 1)]
                                 :values [0.5]}
                             :d {:name   :default :type :cdr
                                 :dates  [(jt/local-date 2017 1 1) (jt/local-date 2049 1 1)]
                                 :values [0.5]}})
        ;deal-run (cn/run-deal jy-bank assmp)

        ]
    ; test current total bond balance
    (is (=
          (spv/query-deal jy-bank [:update :bond :sum-current-balance]))
        (+ 10.1e8 10.1e8 1299999999.72))

    (is (=
          (spv/query-deal jy-bank [:update :pool :sum-current-balance]))
        (+ 101e7 101e7))

    ;(is (<                                                  ;TBD
    ;      (spv/query-deal jy-bank [:projection :bond :sum-current-balance]))
    ;    100)

    ;(comment
      ;(prn  " TOTOAL POOL BALANCE: "                                             ; TBD
       ;     (spv/query-deal deal-run [:projection :pool :sum-current-balance])
       ;   )
    ;  )
    )

  )







;test build deal ,test_001
(comment
  (deftest t-build-deal
    (let [m
          {:info
                      {:dates
                       {:closing-date       (jt/local-date 2017 1 1)
                        :first-collect-date (jt/local-date 2017 3 1)
                        :collect-interval   :M
                        :stated-maturity    (jt/local-date 2025 1 1)
                        :settle-date        (jt/local-date 2017 4 1)
                        :first-payment-date (jt/local-date 2017 5 26)
                        :payment-interval   :Q
                        :delay-days         26}
                       }
           :status    {:update-date (jt/local-date 2019 3 6)}
           :pool      nil
           :waterfall nil
           :bond      nil}

          tdeal (dl/build-deal m)
          ]
      ; test on original info
      (let [di (:info tdeal)
            p-intervals (:p-collection-intervals di)
            b-agg-intervals (:b-aggregate-intervals di)
            b-payment-dates (:b-payment-dates di)

            ]
        (is (= (ffirst p-intervals) (jt/local-date 2017 1 1)))
        (is (= (second (last p-intervals)) (jt/local-date 2024 11 30)))


        (is (= (ffirst b-agg-intervals) (jt/local-date 2017 1 1)))
        (is (= (second (first b-agg-intervals)) (jt/local-date 2017 4 30)))

        (is (= (first b-payment-dates) (jt/local-date 2017 5 26)))
        (is (= (last b-payment-dates) (jt/local-date 2025 1 1)))

        )

      ; test on update info
      (let [ds (:status tdeal)
            p-rest-coll-intervals (:p-rest-collection-intervals ds)
            b-rest-payment-dates (:b-rest-payment-dates ds)
            ]
        (is (= (ffirst p-rest-coll-intervals) (jt/local-date 2019 4 1)))
        (is (= (first (last p-rest-coll-intervals)) (jt/local-date 2024 11 1)))

        (is (= (first b-rest-payment-dates) (jt/local-date 2019 5 26)))
        (is (= (last b-rest-payment-dates) (jt/local-date 2025 1 1)))

        )
      )
    )

  )

(deftest formula-eval
  (let [for-1 "[:a :b :c] + [:c :d]"
        vs-1 ["1" "2"]
        ]
    ;(prn (spv/eval-formula for-1 vs-1))
    ;(is (= "1 + 2" (spv/eval-formula for-1 vs-1)))
    )
  )