(ns clojucture.bond
  (:require [clojucture.type :as t]
            [java-time :as jt]
            [clojucture.account :as acc]
            [clojucture.util :as util]
            [clojucture.util :as u])
  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn]
    [tech.tablesaw.columns AbstractColumn]
    [org.apache.commons.math3.complex Complex]
    [java.time Period LocalDate ])
  )

(defrecord bond
  [ info balance rate stmts last-payment-date interest-arrears principal-loss
   ]
  t/Bond
  (cal-due-principal [ x d ]
    (case (:type info)
      :sequence
        (+ balance principal-loss)
      :schedule
        (let [ prin-due (u/find-first-in-vec d  (info :amortization-schedule) :dates = :after) ]
            (+ (:principal prin-due) principal-loss))
      )
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
          principal-loss-amt (max (- (.cal-due-principal x d) principal-amount) 0)
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
       (->bond info new-balance rate  (conj stmts int-new-stmt prin-new-stmt) d int-arrears principal-loss-amt)
       new-principal
       new-interest
       ]
      )
    )
  )