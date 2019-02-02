(ns clojucture.expense
  (:require [clojucture.type :as t]
            [clojucture.account :as acc]
            [clojucture.core :as ccore]
            [java-time :as jt]
            [clojucture.util :as util])
  (:import [java.time LocalDate Period  ]
           )
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
         draw-amount (min (.balance acc) due-amount)
         new-acc (.try-withdraw acc d (:info acc) draw-amount )
        ]
    [
     new-acc
     (.receive expense d draw-amount )
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
      )
  (receive [ x d amount ]
    (let [ pay-to-arrears (min arrears amount)
          pay-to-expense (- amount pay-to-arrears)]
      (-> x
          (assoc :arrears (- arrears pay-to-arrears))
          (assoc :last-paid-date d)
          (assoc :stmt
                 (conj stmt
                       (ccore/->stmt d nil :expense pay-to-expense nil)
                       (ccore/->stmt d nil :expense-arrears pay-to-arrears nil) ))
          )
      )
    )
  )

(defrecord pct-expense-by-rate
  [ info stmt ^LocalDate last-paid-date ^Double arrears ]
  t/Liability
  (cal-due-amount [ x d base ]
    (+ (* base (info :pct)) arrears)
    )
  (receive [ x d amount ]
    (let [ pay-to-arrears (min arrears amount)
          pay-to-expense (- amount pay-to-arrears)]
      (-> x
          (assoc :arrears (- arrears pay-to-arrears))
          (assoc :last-paid-date d)
          (assoc :stmt
                 (conj stmt
                       (ccore/->stmt d nil :expense pay-to-expense nil)
                       (ccore/->stmt d nil :expense-arrears pay-to-arrears nil) ))
          )
      )
    )
  )



(defrecord pct-expense
  [ info stmt ^LocalDate last-paid-date ^Double arrears ]
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
  (receive [ x d amount]
    (let [ pay-to-arrears (min arrears amount)
           pay-to-expense (- amount pay-to-arrears)]
      (-> x
          (assoc :arrears (- arrears pay-to-arrears))
          (assoc :last-paid-date d)
          (assoc :stmt
                 (conj stmt
                       (ccore/->stmt d nil :expense pay-to-expense nil)
                       (ccore/->stmt d nil :expense-arrears pay-to-arrears nil) ))
          )
      )
    )
  )

(defrecord amount-expense
  [ info stmt ^LocalDate last-paid-date ^Double balance ]
  t/Liability
  (cal-due-amount [ x d ]
    balance
    )
  (receive [ x d amount]
    (if (> amount balance)
      (throw (Exception. "Expense paid over balance"))
      (-> x
          (assoc :balance (- balance amount))
          (assoc :last-paid-date d)
          (assoc :stmt (conj stmt (ccore/->stmt d nil :expense amount nil) ))
          )
      )
    )
  )