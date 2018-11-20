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
    (let [ new-balance (- balance principal)
           int-due     (.cal-due-interest x d )
           int-arrears (- int-due interest)
           int-new-stmt (acc/->stmt d :from :interest  interest  nil)
           prin-new-stmt (acc/->stmt d :from :principal  principal  nil)
          ]
      (->sequence-bond info new-balance rate  (conj stmts int-new-stmt prin-new-stmt) d int-arrears nil)
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