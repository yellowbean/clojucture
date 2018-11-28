(ns clojucture.bond_test
  (:require [clojure.test :refer :all]
            [clojucture.bond :as bnd]
            [clojucture.util :as util]
            [clojucture.account :as acc]
            [java-time :as jt])
  )

(defn close? [tolerance x y]
  (< (Math/abs (- x y)) tolerance))

(deftest test-seq-bond
  (let [ seq-bond (bnd/->bond {:day-count :ACT_365 :type :sequence } 1000 0.08 [] (jt/local-date 2018 1 1)
                    0  0 )
         prin-acc (acc/->account :t :principal 300 [])
         int-acc (acc/->account :t :interest 1000 [])
         [ seq-bond-2 prin-acc-2 int-acc-2 ]  (.receive-payments seq-bond (jt/local-date 2018 4 1) prin-acc int-acc)

        ]
    ;;
    (is (= (.cal-due-principal seq-bond (jt/local-date 2018 4 1) ) 1000 ))
    (is (close? 0.001 (.cal-due-interest seq-bond (jt/local-date 2018 6 1) ) 33.09589041  ))
    ;;
    (is (= (:balance seq-bond-2 ) 700))
    (let [ new-int-stmt (first (:stmts seq-bond-2))
           new-prin-stmt (second (:stmts seq-bond-2))]
      (is (= (:date new-int-stmt) (jt/local-date 2018 4 1)))
      (is (= (:from new-int-stmt) :from ))
      (is (= (:to new-int-stmt) :interest))
      (is (= (:to new-prin-stmt) :principal))

      (is (= (:amount new-prin-stmt) 300))
      (is (close? 0.001 (:amount new-int-stmt) 19.72602))
      )

    ))


(deftest test-schedule-bond
  (let [ balance-schedule [{:dates (jt/local-date 2018 1 1) :principal 4000}
                           {:dates (jt/local-date 2018 6 1) :principal 3000}
                           {:dates (jt/local-date 2018 12 1) :principal 2000}]
         sche-bond (bnd/->bond { :type :schedule :amortization-schedule balance-schedule} 5000 0.07 [] (jt/local-date 2018 5 1) 0 0)]
    (is (= 4000 (.cal-due-principal sche-bond (jt/local-date 2018 1 1)) ))
    (is (= 3000 (.cal-due-principal sche-bond (jt/local-date 2018 6 1)) ))
    )
)
