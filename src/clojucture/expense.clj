(ns clojucture.expense
  (:require [clojucture.type :as t]
            [clojucture.account :as acc]
            [java-time :as jt]
            [clojucture.util :as util])
  (:import [java.time LocalDate Period  ])
  )


(defn pay-expense-at-base
  [ d acc expense base ]
   (let [ due-amount (.cal-due-amount expense d base)
         new-acc (.try-withdraw acc d (:info acc) due-amount )
         paid-amount (Math/abs (:amount (.last-txn new-acc)))
         new-arrears (- due-amount paid-amount)
         ]
     [
      new-acc
      (-> expense
          (assoc :arrears new-arrears)
          (assoc :last-paid-date d))
      ]
     )
  )

(defn pay-expense
  ([ d acc expense ]
  (let [ due-amount (.cal-due-amount expense d )
         new-acc (.try-withdraw acc d (:info acc) due-amount )
         paid-amount (Math/abs (:amount (.last-txn new-acc)))
         new-balance (- (:balance expense) paid-amount)
         new-arrears (- due-amount paid-amount)
        ]
    [
     new-acc
     (-> expense
         (assoc :balance new-balance)
         (assoc :arrears new-arrears)
         (assoc :last-paid-date d))
     ]
    ))
  )



(defrecord pct-expense-by-amount
  [ info stmt ^LocalDate last-paid-date ^Double arrears ]
  t/Liability
  (cal-due-amount [ x d base ]
      (-> (util/get-period-rate
            (Period/between last-paid-date d ) (info :pct) (info :day-count))
          (* base)
          (+ arrears)
          )
      ))

(defrecord pct-expense-by-rate
  [ info stmt ^LocalDate last-paid-date ^Double arrears ]
  t/Liability
  (cal-due-amount [ x d base ]
    (+ (* base (info :pct)) arrears)
    )
  )



(defrecord pct-expense [ info stmt ^LocalDate last-paid-date ^Double arrears ]
  t/Liability
  (cal-due-amount [ x d base ]
    (case (info :type)
      :yearly
        (-> (util/get-period-rate
              (Period/between last-paid-date d ) (info :pct) (info :day-count))
            (* base)
            (+ arrears)
            )
      :one-off
        (+ (* base (info :pct)) arrears)
      )
    )
  )

(defrecord amount-expense [ info ^Double balance stmt ^Double arrears ]
  t/Liability
  (cal-due-amount [ x d ]
    balance
    )
  )