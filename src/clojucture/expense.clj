(ns clojucture.expense
  (:require [clojucture.type :as t]
            [clojucture.account :as acc]
            [java-time :as jt]
            [clojucture.util :as util])
  (:import [java.time LocalDate Period  ])
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
        (* base (info :pct))
      )

    )

  (receive [ x d base a ]
   (let [ total-due (+ (.cal-due-amount x d base) arrears )
         available-payment (min (:balance a) total-due )
         new-stmt (acc/->stmt d (:name a) (:name info) available-payment nil)
         new-arrears (- total-due available-payment)
         ]
     [
      (->pct-expense info (conj stmt new-stmt) d new-arrears)
      (.withdraw a d (:name info) available-payment)
      ]
     )
    )
  )



(defrecord amount-expense [ info ^Double balance stmt ^Double arrears ]
  t/Liability
  (cal-due-amount [ x d ]
    balance
    )
  (receive [ x  d  a ]
    (let [ total-due (+ (.cal-due-amount x d ) arrears )
          available-payment (min (:balance a) total-due )
          new-stmt (acc/->stmt d (:name a) (:name info) available-payment nil)
          new-arrears (- total-due available-payment)
         ]
      [
       (->amount-expense info (- balance available-payment) (conj stmt new-stmt) new-arrears)
       (.withdraw a d (:name info) available-payment)
       ]
      )
    )
  )