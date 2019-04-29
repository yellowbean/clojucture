(set! *warn-on-reflection* true)
(ns clojucture.pool
  (:require [java-time :as jt]
            [clojucture.util :as u]
            [clojucture.type :as t]
            [clojucture.asset :as a]
            )
  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn StringColumn BooleanColumn]
    (clojucture RateAssumption)))


(defprotocol Pool
  (project-cashflow [ x assump ] )
  (collect-cashflow [ x assump interval ] )
  )


(defrecord pool
  [ assets ]
  Pool
  (project-cashflow [ x assump ]
    (let [ total-balance (reduce + (map #(.remain-balance %) assets))
           asset-cashflow (map #(.project-cashflow % assump ) assets )
           cfs         (reduce u/combine-cashflow asset-cashflow)
           prin-ary  (-> (.column ^Table cfs "principal" ) (.asDoubleArray))
           balance-ary (u/gen-balance ^"[D" prin-ary ^Double total-balance)
           balance-col (DoubleColumn/create "balance" ^"[D"  balance-ary)
          ]
      (do
        (.removeColumns ^Table cfs ^"[Ljava.lang.String;" (into-array String ["balance"]))
        (.addColumns ^Table cfs (into-array DoubleColumn [balance-col ]))))
  )
  (collect-cashflow [ x assump collect-intervals ]
    (u/agg-cashflow-by-interval
      (.project-cashflow x assump) collect-intervals))
  )


