(ns clojucture.waterfall
  (:require
    [clojure.core.match :as m])
  )

(defprotocol Waterfall
  (validate? [ x ])
  (pick-seq [ x ])
  (run [ x ] )

  )


(defrecord payment-tree
  [ s & payment-sequences ]
  )


(defrecord payment-sequence
  []
  )

(derive ::deal-default? ::branch)
(derive ::deal-accerate? ::branch)


; z/xml-zip

(comment
  (def simple-tree
    (if-not :deal-default?
      (if-not :deal-accerate?
        simple-sequence
        simple-sequence2
        )
      simple-sequence3
      )
    )

  (def simple-tree2
    [
     ::deal-default?
      [:seq-def]

     [::deal-accerate?
       [:seq-acc]
       [:seq-normal]
      ]
     ]
    )
  (def simple-sequence3
    [
     :deal-default?
     [:def-seq
      ]
     [
      :deal-acc？
      [ :ass-seq ]
      [ :norm-seq ]
      ]
     ]
    )

  (def simple-sequence
    {
     :deal-default?
      [
         (list :cash :expense :tax )
         (list :cash :expense :servicing-fee)
         (list :cash :expense :manage-fee)
         (list :cash :interest :A)
         (list :cash :principal :A)
         (list :cash :transfer :reserve)
       ]
      :deal-acc？ [
         (list :cash :expense :tax )
         (list :cash :expense :servicing-fee)
         (list :cash :expense :manage-fee)
         (list :cash :interest :B)
         (list :cash :principal :B)
         (list :cash :transfer :reserve)
       ]
     }
    )
  )


(defn choose-seq [ t d ]
  true
  )


(defn walk-waterfall [ waterfall-tree deal ]
  (loop [  wt waterfall-tree  d deal ]

    )
  )


(defn execute-instruction [ payment-action deal]
  "execute current payment action on current deal status"
  (let [ {:accounts accounts }  deal
         [ source action-type to  & constrain ]  payment-action
        ]

    )
  )




(defrecord waterfall
  Waterfall
  [ payment-tree deal ]
  (validate?
    (let [ {:accounts accounts :expenses expenses :triggers triggers
            :bonds bonds} deal
           accs-in-seq  (-> (map #(first %) payment-sequence) (set))
           acc-avail (map #(:name %) accounts)

           exps-actions (filter #(= (second %) :expense) payment-sequence)
           exp-in-seq (for [ [ _ _ z] exps-actions] z)
           exp-avail (map #(:name %) accounts)

           bond-actions (filter #(or (= (second %) :interest) (= (second %) :principal) ) payment-sequence)
           bond-in-seq (for [ [ _ _ z] bond-actions] z)
           bond-avail (map #(:name %) bonds)
          ]
      (every? true?
              [(clojure.set/subset? accs-in-seq acc-avail)
               (clojure.set/subset? exp-in-seq exp-avail)
               (clojure.set/subset? bond-in-seq bond-avail)
               ]
        )
      )
    )
  (pick-seq
    nil

    )


  (run
    (let
      [ branch-sequence  ]


      )
    )
  )


