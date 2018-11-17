(ns clojucture.expense
  (:require [clojucture.type :as t]
            [java-time :as jt]
            [clojucture.util :as util])
  (:import [java.time LocalDate Period  ])
  )


(defrecord pct-expense [ name ^Double base ^Double pct day-count ^LocalDate last-paid-date ^Double arrears ]
  t/Liability
  (cal-due-amount [ x d]
     (-> (util/get-period-rate
            (Period/between last-paid-date d ) pct day-count)
           (* base)
         (+ arrears)
       )
    )
  (receive [ x  d receive-amount]
   (let [ new-arrears (+ arrears (- (.cal-due-amount x d) receive-amount) ) ]
     (->pct-expense name base pct day-count d new-arrears )
     )
    )
  )



(defrecord amount-expense [ name ^Double origin-balance ^Double balance ]
  t/Liability
  (cal-due-amount [ x  d]
    balance
    )
  (receive [ x  d  receive-amount ]
    (if (> receive-amount balance)
      (throw (Exception.
               (format "Maxium Amount to received:%f" receive-amount)))
      (->amount-expense name origin-balance (- balance receive-amount))
      )
    )
  )