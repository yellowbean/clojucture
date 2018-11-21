(ns clojucture.bond
  (:require [clojucture.type :as t]
            [java-time :as jt]
            [clojucture.account :as acc ]
            [clojucture.util :as util])
  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn]
    [tech.tablesaw.columns AbstractColumn]
    [org.apache.commons.math3.complex Complex]
    [java.time Period LocalDate ])
  )


(defrecord sequence-bond
  [ info balance rate stmts last-payment-date interest-arrears opt
   ]
  t/Bond
  (cal-due-principal [ x ]
    balance
    )
  (cal-due-interest [ x d ]
    (let [int-due-rate (util/cal-period-rate last-payment-date d rate (info :day-count))
          int-due (* balance int-due-rate)]
      (+ int-due interest-arrears)
      )
    )
  (receive-payments [ x d principal interest ]
    (let [
           principal-amount (min (:balance principal) balance)
           new-principal  (.withdraw principal d :bond-principal principal-amount)
           new-balance (- balance principal-amount)


           int-due     (.cal-due-interest x d )
           interest-amount (min (:balance interest) int-due )
           new-interest (.withdraw interest d :bond-interest interest-amount)
           int-arrears (- int-due interest-amount)

           int-new-stmt (acc/->stmt d :from :interest  interest-amount  nil)
           prin-new-stmt (acc/->stmt d :from :principal  principal-amount  nil)
          ]
      [
       (->sequence-bond info new-balance rate  (conj stmts int-new-stmt prin-new-stmt) d int-arrears nil)
       new-principal
       new-interest
       ]
      )
    )
  )

(defrecord schedule-bond
  [ info balance rate stmts last-payment-date interest-arrears opt ]



  )


;(defn new-bond [ input-bond-list ]
;  (let [ r (atom []) ]
;    (doseq [ b (first input-bond-list) ]
;      (swap! r conj
;       (sequence-bond. {:name (get-in b [:attrs :name])
;                        :original-balance (get-in b [:content :name])
;                        })
    ;(swap! r conj
      ;       (account. (get-in b [:attrs :name]) nil (Float. (get-in a [:attrs :init])) [])))
;    @r
;    )))