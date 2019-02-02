(ns clojucture.bond
  (:require [clojucture.type :as t]
            [clojucture.core :as c]
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


(defn -amortize [ bond d amt loss ]
  (let [ new-stmt (c/->stmt d :from :principal amt nil) ]
    (->
      (update bond :balance - amt )
      (update :stmts conj new-stmt )
      (assoc :principal-loss loss)
      (assoc-in [:last-payment-date :principal] d)
    )
  ))

(defn -pay-interest [ bond d amt arrears ]
  (let [ new-stmt (c/->stmt d :from :interest amt nil) ]
    (->
      (update bond :stmts conj new-stmt )
      (assoc :interest-arrears arrears)
      (assoc-in [:last-payment-date :int ] d )
      )
    ))

(defrecord sequence-bond
  [info balance rate stmts last-payment-date interest-arrears principal-loss ]
  t/Bond
  (cal-due-principal [ x d ]
      (+ balance principal-loss))

  (cal-due-interest [ x d ]
    (u/-cal-due-interest balance (:int last-payment-date) d (info :day-count) rate interest-arrears))
  )

(defrecord schedule-bond
  [info balance rate stmts last-payment-date interest-arrears principal-loss ]
  t/Bond
  (cal-due-principal [ x d ]
    (let [ prin-due (u/find-first-in-vec d  (info :amortization-schedule) :dates = :after) ]
      (+ (:principal prin-due) principal-loss)))

  (cal-due-interest [ x d ]
    (u/-cal-due-interest balance (:int last-payment-date) d (info :day-count) rate interest-arrears))
  )


(defn pay-bond-interest [ d acc bond ]
  (let [ due-int (.cal-due-interest bond d)
         acc-after-paid (.try-withdraw acc d (:name bond) due-int)
         interest-paid (Math/abs (:amount (.last-txn acc-after-paid)))
         interest-arrears (max 0 (- due-int interest-paid)) ]

    [acc-after-paid
     (-pay-interest bond d interest-paid interest-arrears) ]
    )
)

(defn pay-bond-principal [ d acc bond ]
  (let [ due-principal (.cal-due-principal bond d)
         acc-after-paid (.try-withdraw acc d (:name bond) due-principal)
         amortized-principal (Math/abs (:amount (.last-txn acc-after-paid)))
         principal-loss (max 0 (- due-principal amortized-principal)) ]
    [ acc-after-paid
      (-amortize bond d amortized-principal principal-loss) ]
    )
  )