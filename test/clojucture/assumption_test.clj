(ns clojucture.assumption_test
  (:require [clojure.test :refer :all]
            [java-time :as jt]
            [clojucture.assumption :as assump])
  )



(def test-curve (assump/gen-curve
                  :一年以内贷款利率
                  [(jt/local-date 2018 1 1) (jt/local-date 2018 6 1)]
                  [0.43 0.49]
                  ))


(def test-float-info {:index :一年以内贷款利率 :margin 0.05 :reset-period 12})

