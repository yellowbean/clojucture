(ns clojucture.assumption_test
  (:require [clojure.test :refer :all]
            [java-time :as jt]
            [clojucture.assumption :as assump])
  (:use midje.sweet))



(def test-curve (assump/gen-curve
                  :一年以内贷款利率
                  [(jt/local-date 2018 1 1) (jt/local-date 2018 6 1)]
                  [0.43 0.49]
                  ))


(def test-float-info {:index :一年以内贷款利率 :margin 0.05 :reset-period 12})


(fact "curve generation validation"
      (first (:一年以内贷款利率 test-curve)) => [(jt/local-date 2018 1 1) 0.43]
      (second (:一年以内贷款利率 test-curve)) => [(jt/local-date 2018 6 1) 0.49]
      )

(fact "apply curve function"
      (assump/apply-curve test-curve {:index :一年以内贷款利率 :margin 0.01} [(jt/local-date 2018 2 1) (jt/local-date 2018 7 1)])

      )

(fact "index curves application"
      ;(.apply-to test-index-curves test-float-info [(jt/local-date 2018 1 1) (jt/local-date 2018 6 1) ]) => [0.43 0.49]
      ;(.apply-to test-index-curves test-float-info [(jt/local-date 2018 1 2) (jt/local-date 2018 6 2) ]) => [0.43 0.49]
      )
