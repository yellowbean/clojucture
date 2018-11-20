(ns clojucture.bond_test
  (:require [clojure.test :refer :all]
            [clojucture.bond :as bnd]
            [java-time :as jt])
  )

(defn close? [tolerance x y]
  (< (Math/abs (- x y)) tolerance))

(deftest test-seq-bond
  (let [ seq-bond (bnd/->sequence-bond {:day-count :ACT_365 } 1000 0.08 [] (jt/local-date 2018 1 1)
                    0 nil )]
    (is (= (.cal-due-principal seq-bond ) 1000 ))

    (is (close? 0.001 (.cal-due-interest seq-bond (jt/local-date 2018 6 1) ) 33.09589041  ))


    ))
