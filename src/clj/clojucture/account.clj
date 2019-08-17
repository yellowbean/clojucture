(ns clojucture.account
  (:require
            )
  (:import
    [java.time LocalDate])
  )

(defprotocol Account
  (withdraw [ x d to amount ])
  (try-withdraw [ x d to amount ])
  (deposit [ x d from amount ])
  (last-txn [ x ])
  )

(defrecord stmt [^LocalDate date from to ^Double amount info])


(defrecord account [name type ^Double balance stmts]
  Account
  (withdraw [x d to amount]
    (.deposit x d to (- amount))
    )
  (try-withdraw [x d to amount]
    (let [max-to-draw (min amount balance)]
      (.deposit x d to (- max-to-draw)))
    )

  (deposit [x d from amount]
    (let [new-statment (->stmt d from :this amount nil)
          new-balance (+ balance amount)]
      (->account name type new-balance (conj stmts new-statment))
      )
    )

  (last-txn [x]
    (last stmts)
    )
  )

(defn transfer-fund
  ([from-acc to-acc ^LocalDate d ]
   (let [ transfer-amt (:balance from-acc)]
     (transfer-fund from-acc to-acc d transfer-amt) ))
  ([from-acc to-acc ^LocalDate d ^Double amount]
   (if (>= (:balance from-acc) amount)
     (dosync
       [(.withdraw from-acc d to-acc amount) (.deposit to-acc d from-acc amount)]
       )
     :not-enough-cash)))

(defn transfer-funds
  [ acc-list to-acc ^LocalDate d]
  (loop [ target-acc to-acc from-acc-list acc-list result [] ]
    (if-let [ from-acc (first from-acc-list)]
      (let [ [new-from new-to] (transfer-fund from-acc target-acc d) ]
        (recur new-to (next from-acc-list) (conj result new-from) ) )
    [result target-acc] )))



