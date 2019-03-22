(ns clojucture.expense
  (:require [clojucture.type :as t]
            [clojucture.account :as acc]
            [clojucture.core :as ccore]
            [java-time :as jt]
            [clojucture.util :as util]
            [clojucture.util :as u]
            [clojure.core.match :as m])
  (:import [java.time LocalDate Period  ]
           )
  )


(defn pay-expense-at-base
  [  d acc expense base ]
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
  ([  d acc expense ]
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

(defn get-base [ deal d e]
  (let [exp-base (:base (:info e))]
    (get-in deal [:status :snapshot d exp-base]) ) )


(defn pay-expense-deal [ deal d acc exp opt]
  (let [ account (get-in deal [:account acc])
        expense (get-in deal [:fee exp])
        base? (contains? (:info expense) :pct) ]
    (as->
      (m/match base?
       true (pay-expense-at-base d account expense (get-base deal d expense))
       false (pay-expense d account expense) )
       [ update-acc update-expense ]
      (-> deal
          (assoc-in [:account acc] update-acc )
          (assoc-in [:fee exp ] update-expense ) ) )
  ))

;"Expense type that due amount is annualized percentage of the base"
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

;"Expense type that due amount is percentage of the base"
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

(defrecord recur-expense
  [ info stmt ^Double arrears ]
  t/Liability
  (cal-due-amount [ x d ]
    (let [ {sd :start-date p :period e-date :end-date } info
          exp-dates (u/gen-dates-range sd p e-date)]
      (if (some #(= % d) exp-dates )
        (:amount info)
        0
        )
      )
    )
  (receive [ x d amount ]
    (let [pay-to-arrears (min arrears amount)
          pay-to-expense (- amount pay-to-arrears)]
      (-> x
          (assoc :arrears (- arrears pay-to-arrears))
          (assoc :stmt (conj stmt
                             (ccore/->stmt d nil :expense pay-to-expense nil)
                             (ccore/->stmt d nil :expense-arrears pay-to-arrears nil) ))

          )
      )
    )
  )