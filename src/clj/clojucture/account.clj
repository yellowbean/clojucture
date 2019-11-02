(ns clojucture.account
  (:require [clojure.core.match :as m])
  (:import
    [java.time LocalDate]))

(defprotocol pAccount
  (withdraw [x d to amount])
  (try-withdraw [x d to amount])
  (deposit [x d from amount])
  (last-txn [x])
  )

;; statement
(defrecord stmt [^LocalDate date from to ^Double amount info])

;; building block functions in account
(defn -withdraw [x ^LocalDate d dest ^Double amount]
  (let [new-stmt (->stmt d :this dest (- amount) nil)]
    (-> x
        (update :balance - amount)
        (update :stmts conj new-stmt))))


(defn -deposit [x ^LocalDate d source ^Double amount]
  (let [new-stmt (->stmt d source :this amount nil)]
    (-> x
        (update :balance + amount)
        (update :stmts conj new-stmt))))


(defrecord account [name type ^Double balance stmts]
  pAccount
  (withdraw [x d to amount]
    (-withdraw x d to amount)
    )

  (try-withdraw [x d to amount]
    (let [max-to-draw (min amount balance)]
      (-withdraw x d to max-to-draw)))

  (deposit [x d from amount]
    (-deposit x d from amount))

  (last-txn [x]
    (last stmts)
    )
  )

;; reserve account
(defrecord reserve-account [name info ^Double balance stmts]
  pAccount
  (withdraw [x d to amount]
    (-withdraw x d to amount))

  (try-withdraw [x d to amount]
    (let [max-to-draw (min amount balance)]
      (-withdraw x d to max-to-draw)))

  (deposit [x d from amount]
    (m/match info
             {:target target-balance}
             (let [upper-limit-to-deposit (max (- target-balance balance) 0)
                   amount-to-deposit (min upper-limit-to-deposit amount)]
               (-deposit x d from amount-to-deposit)) )
    )

  (last-txn [x]
    (last stmts))
  )

(defn transfer-fund
  "transfer cash from account to another , with optional amount specified"
  ([from-acc to-acc ^LocalDate d]
   (let [transfer-amt (:balance from-acc)]
     (transfer-fund from-acc to-acc d transfer-amt)))
  ([from-acc to-acc ^LocalDate d ^Double amount]
   (if (>= (:balance from-acc) amount)
     (let [updated-to-acc (.deposit to-acc d from-acc amount)
           amt-to-wd (:amount (last-txn updated-to-acc))
           updated-from-acc (.withdraw from-acc d to-acc amt-to-wd)]
       [ updated-from-acc updated-to-acc ]
       )
     :not-enough-cash)))

(defn transfer-funds
  [accs-map to-acc ^LocalDate d]
  (loop [target-acc to-acc from-acc-map accs-map result {}]
    (if-let [ [ k f-acc] (first from-acc-map)]
      (let [[new-from new-to] (transfer-fund f-acc target-acc d)]
        (recur new-to (next from-acc-map) (assoc result k new-from)))
      [result target-acc])))


(defn update-target-account [ new-source-acc target-account  ]
  "When a source account transfer cash to target account , create a new target account base on `delta` of new & old source account "
  (let [  last-stmt (last-txn new-source-acc)
        {txn-date :date fr :from to :to amt :amount } last-stmt ]
    (-deposit target-account txn-date nil (- amt ))
  ))
