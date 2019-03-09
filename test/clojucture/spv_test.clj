(ns clojucture.spv_test
  (:require
    [clojure.test :refer :all]
    [clojucture.spv :as dl ]
    [java-time :as jt]))



;test build deal ,test_001

(deftest t-build-deal
  (let [ m
        {:info
          {:dates
            {:closing-date (jt/local-date 2017 1 1)
            :first-collect-date (jt/local-date 2017 3 1)
            :collect-interval :M
            :stated-maturity (jt/local-date 2025 1 1)
            :settle-date (jt/local-date 2017 4 1)
            :first-payment-date (jt/local-date 2017 5 26)
            :payment-interval :Q
            :delay-days 26}
            }
         :status {:update-date (jt/local-date 2019 3 6)}
         :pool  nil
         :waterfall  nil
         :bond nil}

        tdeal (dl/build-deal m)
        ]
    ; test on original info
    (let [ di (:info tdeal)
          p-intervals (:p-collection-intervals di)
          b-agg-intervals (:b-aggregate-intervals di)
          b-payment-dates (:b-payment-dates di)

          ]
      (is (= (ffirst p-intervals) (jt/local-date 2017 1 1)))
      (is (= (second (last p-intervals))  (jt/local-date 2024 11 30)))


      (is (= (ffirst b-agg-intervals) (jt/local-date 2017 1 1)))
      (is (= (second (first b-agg-intervals)) (jt/local-date 2017 4 30)))

      (is (= (first b-payment-dates) (jt/local-date 2017 5 26)))
      (is (= (last b-payment-dates) (jt/local-date 2025 1 1)))

      )

    ; test on update info
    (let [ ds (:status tdeal)
          p-rest-coll-intervals (:p-rest-collection-intervals ds)
          b-rest-payment-dates (:b-rest-payment-dates ds)
          ]
      (is (= (ffirst p-rest-coll-intervals) (jt/local-date 2019 4 1 )))
      (is (= (first (last p-rest-coll-intervals))  (jt/local-date 2024 11 1)  ))

      (is (= (first b-rest-payment-dates) (jt/local-date 2019 5 26)  ))
      (is (= (last b-rest-payment-dates) (jt/local-date 2025 1 1) ))

      )
    )
  )