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
  (let [ seq-bond (bnd/->sequence-bond {:day-count :ACT_365} 1000 0.08 [] {:int (jt/local-date 2018 1 1) :principal (jt/local-date 2018 1 1)}
                    0  0 )
         prin-acc (acc/->account :t :principal 300 [])
         int-acc (acc/->account :t :interest 1000 [])
         [ prin-acc-2 seq-bond-2  ]  (bnd/pay-bond-principal (jt/local-date 2018 4 1) prin-acc seq-bond   )
         [ int-acc-2 seq-bond-3  ]  (bnd/pay-bond-interest (jt/local-date 2018 4 1) int-acc seq-bond   )

        ]
    ;;
    (is (= (.cal-due-principal seq-bond (jt/local-date 2018 4 1) ) 1000 ))
    (is (close? 0.001 (.cal-due-interest seq-bond (jt/local-date 2018 6 1) ) 33.09589041  ))
    ;;
    (is (= (:balance seq-bond-2 ) 700))
    (is (= (:principal-loss seq-bond-2 ) 700))
    (let [ new-prin-stmt (.last-txn prin-acc-2) ]
      (is (= (:amount new-prin-stmt) -300))
      )

    (is (= (:balance seq-bond-3 ) 1000))
    (let [ new-int-stmt (.last-txn int-acc-2) ]
      (is (close? 0.001 (:amount new-int-stmt) -19.72602))
      )

    ))

(deftest test-schedule-bond
  (let [ balance-schedule [{:dates (jt/local-date 2018 1 1) :principal 4000}
                           {:dates (jt/local-date 2018 6 1) :principal 3000}
                           {:dates (jt/local-date 2018 12 1) :principal 2000}]
         sche-bond (bnd/->schedule-bond { :amortization-schedule balance-schedule} 5000 0.07 []  {:int (jt/local-date 2018 5 1) :principal (jt/local-date 2018 5 1)} 0 0)]
    (is (= 4000 (.cal-due-principal sche-bond (jt/local-date 2018 1 1)) ))
    (is (= 3000 (.cal-due-principal sche-bond (jt/local-date 2018 6 1)) ))
    )
)

(deftest test-amortize-bond
  (let [ seq-bond (bnd/->sequence-bond {:day-count :ACT_365 } 1000 0.08 [] {:int (jt/local-date 2018 1 1) :principal (jt/local-date 2018 1 1)}
                              0  0 )

         seq-bond-amortized (bnd/-amortize seq-bond (jt/local-date 2018 1 1) 300 0)
         new-stmt  (first (:stmts seq-bond-amortized))]
     (is (= 700 (:balance seq-bond-amortized)))
     (is (= 300 (:amount new-stmt)))
    )
  )
