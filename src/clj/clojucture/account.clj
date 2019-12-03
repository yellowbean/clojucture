(ns clojucture.account
  (:require
    [clojure.core.match :as m]
    [com.rpl.specter :as s]
    [clojucture.util :as u])
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
  (let [new-stmt (->stmt d (:name x) dest (- amount) nil)]
    (if (zero? amount)
      x ; it doesn't have to create a new transaction if amount = 0
      (-> x
          (update :balance - amount)
          (update :stmts conj new-stmt)) ) ))


(defn -deposit [x ^LocalDate d source ^Double amount]
  (let [new-stmt (->stmt d source (:name x) amount nil)]
    (if (zero? amount)
      x ; it doesn't have to create a new transaction if amount = 0
      (-> x
          (update :balance + amount)
          (update :stmts conj new-stmt)))))

(defn select-stmts [x e]
  (let [stmts (:stmts x)]
    (m/match e
             {:to target}
             (s/select [s/ALL #(= (:to %) target)] stmts)

             {:from source}
             (s/select [s/ALL #(= % (:from %) source)] stmts)

             :else :not-match-stmts-pattern
             )

    )
  )

(defn sum-stmts [x]
  (if (nil? x)
    0
    (reduce + (s/select [s/ALL :amount] x)))
  )


(defrecord account [name type ^Double balance stmts]
  pAccount
  (withdraw [x d to amount]
    (-withdraw x d to amount)
    )

  (try-withdraw [x d to amount]
    (let [max-to-draw (min amount balance)]
      (-withdraw x d to max-to-draw)))

  (deposit [x d from amount]
    (-deposit x d from amount)
    )

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
               (-deposit x d from amount-to-deposit)))
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
     (let [updated-to-acc (.deposit to-acc d (:name from-acc) amount)
           amt-to-wd (:amount (last-txn updated-to-acc))
           updated-from-acc (.withdraw from-acc d (:name to-acc) amt-to-wd)]
       [updated-from-acc updated-to-acc]
       )
     :not-enough-cash)))

(defn transfer-funds
  [accs-map to-acc ^LocalDate d]
  (loop [target-acc to-acc from-acc-map accs-map result {}]
    (if-let [[k f-acc] (first from-acc-map)]
      (let [[new-from new-to] (transfer-fund f-acc target-acc d)]
        (recur new-to (next from-acc-map) (assoc result k new-from)))
      [result target-acc])))


(defn update-target-account [new-source-acc target-account]
  "When a source account transfer cash to target account , create a new target account base on `delta` of new & old source account "
  (let [last-stmt (last-txn new-source-acc)
        {txn-date :date fr :from to :to amt :amount} last-stmt]
    (-deposit target-account txn-date (:name new-source-acc) (- amt))
    ))


(defn setup-account [x]
  (m/match x
           {:name n :balance b :stmts txn}
           (map->account {:name n :type nil :stmts txn :balance b})
           {:name n :balance b}
           (map->account {:name n :type nil :stmts [] :balance b})

           )
  )

(defn view-stmts [stmts]
  (let [
        ds (s/select [s/ALL :date] stmts)
        fs (s/select [s/ALL :from s/NAME] stmts)
        ts (s/select [s/ALL :to s/NAME] stmts)
        amts (s/select [s/ALL :amount] stmts)
        ; :info field in stmt structure is not populated yet
        ]
    ;ds
    (u/gen-table "statements" [{:name :date :type :date :values ds}
                               {:name :from :type :string :values fs}
                               {:name :to :type :string :values ts}
                               {:name :amount :type :double :values amts}
                               ])

    )

  )