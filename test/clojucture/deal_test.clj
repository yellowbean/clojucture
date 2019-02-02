(ns clojucture.deal_test
  (:require
    [clojure.test :refer :all]
    [clojucture.deal :as dl ]
    [java-time :as jt]))

(deftest testing-gen-collect-dates
  (let [di { :stated-maturity (jt/local-date 2025 1 1)
            :collect-interval :M
            :closing-date (jt/local-date 2019 1 1)
            :first-collect-date (jt/local-date 2019 3 1) }
        dv (dl/gen-pool-collect-interval di)
        ]
    (is (= (ffirst dv) (jt/local-date 2019 1 1)))
    (is (= (second (first dv)) (jt/local-date 2019 2 28)))

    (is (= (first (nth dv 1)) (jt/local-date 2019 3 1)))
    (is (= (second (nth dv 1)) (jt/local-date 2019 3 31)))

    (is (jt/before? (second (last dv)) (di :stated-maturity)))
    )
  )
