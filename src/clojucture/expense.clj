(ns clojucture.expense
  (:require [clojucture.type :as t]
            [clojucture.account :as acc]
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

(comment
(defn pay-expense-prorata [ d acc expenses ]
  (let [ total-due-amount (+ (map #(.cal-due-amount % d ) expenses))
         enough-pay? (<= total-due-amount (.balance acc))
         per-unit (/ (.balance acc) total-due-amount )
         new-acc (.try-withdraw acc d (:info acc) (min (.balance acc ) total-due-amount) )
        ]
      [
       new-acc
       (if enough-pay?
         (map #(-> %
                   (assoc :balance new-balance)
                   (assoc :arrears new-arrears)
                   (assoc :last-paid-date d))  expenses )
         )

       ]
    )
  )
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
  )

(defrecord amount-expense
  [ info stmt ^Double balance ]
  t/Liability
  (cal-due-amount [ x d ]
    balance
    )
  (receive [ x d amount]
    (if (> amount balance)
      (throw (Exception. "Expense paid over balance"))
      (-> x
          (assoc :balance (- balance amount))
          (assoc :stmt (conj stmt (acc/->stmt d nil :expense amount nil) ))
          )
      )
    )
  )