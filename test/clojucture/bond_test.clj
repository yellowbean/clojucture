(ns clojucture.bond_test
  (:require [clojure.test :refer :all]
            [clojucture.bond :as bnd]
            [clojucture.account :as acc]
            [java-time :as jt])
  )

(defn close? [tolerance x y]
  (< (Math/abs (- x y)) tolerance))

(deftest test-seq-bond
  (let [ seq-bond (bnd/->sequence-bond {:day-count :ACT_365 } 1000 0.08 [] (jt/local-date 2018 1 1)
                    0 nil )
         prin-acc (acc/->account :t :principal 300 [])
         int-acc (acc/->account :t :interest 1000 [])
         [ seq-bond-2 prin-acc-2 int-acc-2 ]  (.receive-payments seq-bond (jt/local-date 2018 4 1) prin-acc int-acc)

        ]
    ;;
    (is (= (.cal-due-principal seq-bond ) 1000 ))
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
