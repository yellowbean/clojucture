(ns clojucture.io.json
  (:require
    [clojucture.tranche :as b ]
    [clojucture.pool :as p ]
    [clojucture.account :as acc ]
    [clojucture.expense :as exp ]
    )
  (:import [tech.tablesaw.api Table])
  (:import
    [clojucture.tranche sequence-bond equity-bond]
        )
  )



(defn view-bond-flow
  ([ d ])
  ([ d b-id-list ])
  )


(defn view-pool-flow
  ([ d ])
  ([ d a-id-list ])
  )
