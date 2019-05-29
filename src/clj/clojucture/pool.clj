(set! *warn-on-reflection* true)
(ns clojucture.pool
  (:require [java-time :as jt]
            [clojucture.util :as u]
            [clojucture.type :as t]
            [clojucture.asset :as a]
            [clojucture.account :as acc]
            [clojure.core.match :as m]
            )
  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn StringColumn BooleanColumn Row]
    [java.time LocalDate]
    (clojucture RateAssumption Cashflow)))


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
    (-> (project-cashflow x assump)
        (u/agg-cashflow-by-interval collect-intervals)
        )
    )
  )

(defn deposit-period-to-accounts [ ^Row current-collection accounts mapping ^LocalDate d]
  "deposit current collection period to accounts"
  (let [ source-fields  (keys mapping) ]
    (loop [  sfs source-fields  accs accounts ]
      (if-let [ this-sf (first sfs) ] ;; pool cashflow field
        (recur
          (rest sfs)
          (let [ deposit-amt (.getDouble current-collection (name this-sf))
                deposit-acc (accs (keyword (mapping this-sf)))]
            (assoc accs
              (:name deposit-acc)
              (.deposit deposit-acc d this-sf deposit-amt)))
          )
        accs
        )
      )
    )
  )

(defn calc-deposit-date [ ^Row r  adj ]
  (println "Getting" r)
  (let [ last-date (.getDate r "ending-date")]
    (m/match adj
       {:delay-days n} (.plusDays last-date n)

      :else nil
      )
    )
  )


(defn deposit-to-accs [ ^Cashflow pool-cf accounts  mapping deposit-adj]
 "deposit cashflow from 'pool-cf' into `accounts` by `rules` "
  (loop [ cr (Row. pool-cf) result-accs accounts ]
    (if-not (.hasNext cr)
      result-accs
      (recur
        (.next cr)
        (deposit-period-to-accounts cr result-accs mapping (jt/local-date 2018 1 1));;(calc-deposit-date cr deposit-adj))
        )
      )
    )
)

