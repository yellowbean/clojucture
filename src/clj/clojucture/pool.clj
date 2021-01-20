(ns clojucture.pool
  (:require [java-time :as jt]
            [clojucture.util :as u]
            [clojucture.util-cashflow :as cfu]
            [clojucture.asset :as a]
            [clojucture.assumption :as assump]
            [clojucture.account :as acc]
            [clojure.core.match :as m])

  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn StringColumn BooleanColumn Row]
    [java.time LocalDate]
    (clojucture RateAssumption Cashflow)))


(defprotocol pPool
  (project-cashflow [x] [x assump])
  (collect-cashflow [x assump interval]))


(defrecord pool                                             ; "A object represent a pool holding assets with a cutoff-date"
  [assets ^LocalDate cutoff-date]
  pPool
  (project-cashflow [x]
    (project-cashflow x [:cdr [0.0] [(jt/local-date 1900 1 1) (jt/local-date 2100 1 1)]]))

  (project-cashflow [x assump]
    (let [
          ;_ (prn "assump passed via apply" assump)
          ;pool-assump (apply assump/gen-pool-assump-df assump)
          pool-assump-list (map #(apply assump/gen-pool-assump-df %) assump)
          asset-cashflows (map #(.project-cashflow % assump) assets) ;project cashflow on each asset
          cfs (reduce #(.append %1 %2) asset-cashflows)     ;aggregate all cashflow for each asset
          ]
      (cfu/sub-cashflow cfs :>= cutoff-date)))

  (collect-cashflow [x assump collect-intervals]
    (let [total-bal (reduce + 0 (map :balance assets))]
      (-> (if (nil? assump)
            (project-cashflow x)
            (project-cashflow x assump))
          (cfu/agg-cashflow-by-interval collect-intervals)
          (cfu/add-end-bal-column total-bal)
          (cfu/add-beg-bal-column total-bal)
          (cfu/drop-rows-if-empty)                          ;trancate empty cashflow at end
          ))
    )
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
     

