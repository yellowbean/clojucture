(ns clojucture.waterfall
  (:require
    [clojure.core.match :as m]
    [clojure.zip :as zip]
    [clojure.string :as str]
    [clojucture.expense :as exp]
    [clojucture.account :as acc]
    [clojucture.tranche :as b]
    [clojucture.spv :as spv]
    [com.rpl.specter :as s]
    )
  (:import (java.time LocalDate)
           (tech.tablesaw.api Row)
           ))




(defn- fee? [d fee-key]
  (s/select-any [:update :expense fee-key] d))

(defn- bond? [d bond-key]
  (s/select-any [:update :bond bond-key] d))

(defn- account? [d acc-key]
  (s/select-any [:update :account acc-key] d))

(defn- all-account? [d acc-keys]
  (every? #(account? d %) acc-keys))


(defn distribute [updating-deal action ^LocalDate pay-date]
  "Execute a `step` or `action` or `change` on the deal map and return with deal updated "
  (m/match (do
             (prn "matching action" action)
             ;(prn "expense keys" (s/select [:update :expense s/MAP-KEYS] updating-deal))
             ;(prn "Distributing")
             action)
           {:from pool-c :to to-acc :fields f-list}         ;transfer cash from pool collection to deal accounts
           (let [
                 current-period (get-in updating-deal [:projection :period] )
                 collection-at-period (spv/get-pool-collection updating-deal current-period)
                 amounts-to-deposit (map #(.getDouble collection-at-period %) f-list)
                 ]
             (s/transform [:projection :account to-acc]
                          #(.deposit % pay-date :pool-collection (reduce + amounts-to-deposit))
                          updating-deal))


           {:from from-acc :to to-acc :formula fml}
           (let [transfer-amt (spv/evaluate-formula updating-deal fml)]
             (s/multi-transform [:projection :account
                                 (s/multi-path
                                   [to-acc (s/terminal #(.deposit % pay-date from-acc transfer-amt))]
                                   [from-acc (s/terminal #(.withdraw % pay-date to-acc transfer-amt))])
                                 ] updating-deal) )


           {:from source-acc :to target-acc :expense (exp-key :guard (partial fee? updating-deal)) :with-limit-percentage pct}
           (let [acc-to-pay (s/select-one [:projection :account source-acc] updating-deal)
                 fee-to-pay (s/select-one [:projection :expense exp-key] updating-deal)
                 [new-acc new-exp] (exp/pay-expense updating-deal pay-date acc-to-pay fee-to-pay {:with-limit-pct pct})
                 ]
             (s/multi-transform [:projection
                                 (s/multi-path
                                   [:account source-acc (s/terminal-val new-acc)]
                                   [:account target-acc (s/terminal #(acc/update-target-account new-acc %))]
                                   [:expense exp-key (s/terminal-val new-exp)]
                                   )] updating-deal) )


           {:from source-acc :to target-acc :expense (fee-key-list :guard seq?)}
           (let [source-acc (s/select-one [:projection :account] updating-deal)
                 exp-map (s/select-one [:projection :expense (s/submap fee-key-list)] updating-deal)
                 [new-acc new-exp-map] (exp/pay-expenses-pr updating-deal pay-date source-acc exp-map nil)
                 ]
             (s/multi-transform [:projection
                                 (s/multi-path
                                   [:account source-acc (s/terminal-val new-acc)]
                                   [:account target-acc (s/terminal #(acc/update-target-account new-acc %))]
                                   [:expense #(merge % new-exp-map)]
                                   )
                                 ] updating-deal) )


           {:from source-acc :to target-acc :expense (fee-key :guard (partial fee? updating-deal))}
           (let [fee-to-pay (s/select-one [:projection :expense fee-key] updating-deal)
                 acc-to-pay (s/select-one [:projection :account source-acc] updating-deal)
                 [new-acc new-exp] (exp/pay-expense updating-deal pay-date acc-to-pay fee-to-pay nil)
                 ]
             (s/multi-transform [:projection
                                 (s/multi-path
                                   [:expense fee-key (s/terminal-val new-exp)]
                                   [:account source-acc (s/terminal-val new-acc)]
                                   [:account target-acc (s/terminal #(acc/update-target-account new-acc %))]
                                   )
                                 ] updating-deal) )


           {:from source-acc :to target-acc :interest (bond-key-list :guard seq?)}
           (let [acc-to-pay (s/select-one [:projection :account source-acc] updating-deal)
                 bonds-to-pay (s/select [:projection :bond (s/submap bond-key-list)] updating-deal)
                 [new-acc new-bnd-map] (b/pay-bond-interest-pr pay-date acc-to-pay bonds-to-pay)
                 ]
             (s/multi-transform
               [:projection
                (s/multi-path
                  [:account source-acc (s/terminal-val new-acc)]
                  [:account target-acc (s/terminal #(acc/update-target-account new-acc %))]
                  [:bond bond-key-list (s/terminal #(merge % new-bnd-map))]
                  )] updating-deal)
             )


           {:from source-acc :to target-acc :interest (bond-key :guard (partial bond? updating-deal))}
           (let [
                 acc-to-pay (s/select-one [:projection :account source-acc] updating-deal)
                 ;bond-to-pay (s/select-one [:projection :bond bond-key s/VAL ] updating-deal)
                 bond-to-pay (get-in updating-deal [:projection :bond bond-key])
                 [new-acc bond-u] (b/pay-bond-interest pay-date acc-to-pay bond-to-pay)
                 ]

             (s/multi-transform
               [:projection
                (s/multi-path
                  [:account source-acc (s/terminal-val new-acc)]
                  [:account target-acc (s/terminal #(acc/update-target-account new-acc %))]
                  [:bond bond-key (s/terminal-val bond-u)])
                ] updating-deal
               )
             )


           {:from source-acc :to target-acc :principal bond-key}
           (let [acc-to-pay (s/select-one [:projection :account source-acc] updating-deal)
                 bond-to-pay (s/select-one [:projection :bond bond-key] updating-deal)
                 ;_ (prn (get-in updating-deal [:projection ]))
                 [new-acc new-bond] (b/pay-bond-principal pay-date acc-to-pay bond-to-pay)
                 ]
             (s/multi-transform
               [:projection
                (s/multi-path
                  [:account source-acc (s/terminal-val new-acc)]
                  [:account source-acc (s/terminal #(acc/update-target-account new-acc %))]
                  [:bond bond-key (s/terminal-val new-bond)] )
                ] updating-deal
               )
             )


           {:from (source-acc :guard (partial account? updating-deal)) :to (target-acc :guard (partial account? updating-deal))}
           (let [acc-to-pay (s/select-one [:projection :account source-acc] updating-deal)
                 acc-to-received (s/select-one [:projection :account target-acc] updating-deal)
                 [new-from new-to] (acc/transfer-fund acc-to-pay acc-to-received pay-date)
                 ]
             (s/multi-transform
               [:projection :account
                (s/multi-path
                  [source-acc (s/terminal-val new-from)]
                  [target-acc (s/terminal-val new-to)])
                ] updating-deal ) )


           {:from (source-acc :guard (partial account? updating-deal)) :to (target-acc :guard (partial account? updating-deal)) :amount (amt :guard number?)}
           (let [acc-to-pay (s/select-one [:projection :account source-acc] updating-deal)
                 acc-to-receive (s/select-one [:projection :account target-acc] updating-deal)
                 [new-from new-to] (acc/transfer-fund acc-to-pay acc-to-receive pay-date amt)
                 ]
             (s/multi-transform
               [:projection :account
                (s/multi-path
                  [source-acc (s/terminal-val new-from)]
                  [target-acc (s/terminal-val new-to)])
                ] updating-deal ) )


           {:from (acc-key-list :guard sequential?) :to (acc-key :guard (partial account? updating-deal))}
           (let [acc-map-to-pay (s/select-one [:projection :account (s/submap acc-key-list)] updating-deal)
                 acc-to-receive (s/select-one [:projection :account acc-key] updating-deal)
                 [new-from-map new-to] (acc/transfer-funds acc-map-to-pay acc-to-receive pay-date) ]
             (s/multi-transform
               [:projection
                (s/multi-path
                  [:account (s/submap acc-key-list) (s/terminal #(merge % new-from-map))]
                  [:account acc-key (s/terminal-val new-to)]
                  )] updating-deal
               )
             )

           :else (throw (Exception. "NOT MATCH ACTION:"))
           )
  )



(defn- is-trigger [n]
  (let [first-elem (-> n first)]
    (if (and (keyword? first-elem) (-> first-elem name (.endsWith "?")))
      true
      false)
    ))


(defn- branching [n deal]
  (let [trg-name (-> n first name)]
    (spv/query-deal deal [:projection :trigger (keyword (str/replace trg-name "?" "")) :status])
    )
  )


(defn walk-waterfall
  ([tree deal ^LocalDate pay-date]
   (walk-waterfall tree deal pay-date 7)) ;maximum steps = 100
  ([tree deal ^LocalDate pay-date ^Integer max-walks]
   (loop [tr  tree updating-deal deal path [] i 0]
     ;(prn "paths " path)
     (if (or (and (= tr tree) (not= i 0)) (> i max-walks))
       ; final result
       [(s/transform [:projection :period] inc updating-deal) path]
       ; looping
       (let [current-node (zip/node tr)]
         (cond ; if it is a trigger branch
           (and (zip/branch? tr) (is-trigger current-node))
           (->
             (if (branching current-node updating-deal)       ;if true, go left node,delete right node ;   false -> go right node, delete left node
                (-> tr zip/down zip/right zip/down zip/right (zip/replace nil) zip/left)
                (-> tr zip/down zip/right zip/down (zip/replace nil) zip/right))
             (recur updating-deal path (inc i)))

           (zip/branch? tr); if it is a branch -> which contains a list of actions to be executed
           (recur (zip/next tr) updating-deal path (inc i))

           (nil? current-node)
           (recur (zip/up tr) updating-deal path (inc i))

           :else ; if it is a node
           (recur (zip/next tr) (distribute updating-deal current-node pay-date) (conj path current-node) (inc i))
           ))
       )
     )
   )
  )
