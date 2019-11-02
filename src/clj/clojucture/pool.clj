(ns clojucture.pool
  (:require [java-time :as jt]
            [clojucture.util :as u]
            [clojucture.util-cashflow :as cfu]
            [clojucture.asset :as a]
            [clojucture.account :as acc]
            [clojure.core.match :as m])

  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn StringColumn BooleanColumn Row]
    [java.time LocalDate]
    (clojucture RateAssumption Cashflow)))


(defprotocol pPool
  (project-cashflow [ x ][ x assump])
  (collect-cashflow [x assump interval]))



(defrecord pool
  [assets  ^LocalDate cutoff-date]
  pPool
  (project-cashflow [ x ]
    (project-cashflow x nil))

  (project-cashflow [ x assump ]
    (let [ asset-cashflows (map #(.project-cashflow % assump) assets)
           cfs (reduce #(.append %1 %2 ) asset-cashflows) ]
      (cfu/sub-cashflow cfs :>= cutoff-date )))

  (collect-cashflow [ x assump collect-intervals ]
    (-> (project-cashflow x assump)
        (uc/agg-cashflow-by-interval collect-intervals)
        (uc/drop-rows-if-empty) ;trancate empty cashflow at end
        ))
  )



(defn deposit-period-to-accounts
  [^Row current-collection accounts mapping ^LocalDate d]
  "deposit current collection period to accounts"
  (let [source-fields (keys mapping)]
    (loop [sfs source-fields accs accounts]
      (if-let [this-sf (first sfs)]                         ;; pool cashflow field
        (recur
          (next sfs)
          (let [deposit-amt (.getDouble current-collection (name this-sf))
                deposit-acc (accs (keyword (mapping this-sf)))]
            (assoc accs
              (:name deposit-acc)
              (.deposit deposit-acc d this-sf deposit-amt))))
        accs))))


(defn calc-deposit-date
  [^Row collection-period adj]
  (let [
        collection-end-date (.getDate collection-period "ending-date")
        deposit-delay-days (:delay-days adj)]
    (jt/plus collection-end-date (jt/days deposit-delay-days))))


(defn deposit-to-accs
  [^Cashflow pool-cf accounts mapping deposit-adj]
  "deposit cashflow from 'pool-cf' into `accounts` by `rules` "
  (loop [cr (Row. pool-cf)
         result-accs accounts]
    (if-not (.hasNext cr)
      result-accs
      (recur
        (.next cr)
        (deposit-period-to-accounts cr result-accs mapping (calc-deposit-date cr deposit-adj))))))

(defn calc-cumulative-amount
  [^Cashflow pool-cf field-name]
  "add a cumulative column from a table's column "
  (let [col (.column pool-cf field-name)
        col-c (.columnCount pool-cf)
        cum-col (.cumSum col)]
    (.insertColumn pool-cf col-c cum-col)))
     

