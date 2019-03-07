(ns clojucture.spv_test
  (:require
    [clojure.test :refer :all]
    [clojucture.spv :as dl ]
    [java-time :as jt]))



;test build deal

(deftest t-build-deal
  (let [ m
        {:info
          {:dates
            {:closing-date (jt/local-date 2017 1 1)
            :first-collect-date (jt/local-date 2017 3 1)
            :collect-interval :M
            :stated-maturity (jt/local-date 2025 1 1)
            :settle-date (jt/local-date 2017 4 1)
            :first-payment-date (jt/local-date 2017 5 1)
            :payment-interval :Q}
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
          b-payment-dates (:b-payment-dates di)]

      (is (= (ffirst p-intervals) (jt/local-date 2017 1 1)))
      (is (= (second (last p-intervals))  (jt/local-date 2025 1 1)))

      (is (= (first b-payment-dates) (jt/local-date 2017 5 1)))
      (is (= (last b-payment-dates) (jt/local-date 2025 1 1)))

      )

    ; test on update info

    )
  )