(ns clojucture.trigger
  (:require [clojure.core.match :as m]
            [clojucture.util-cashflow :as cfu]
            [clojucture.util :as u]
            [clojucture.spv :as spv]
            )
  (:import (clojucture RateAssumption)
           (tech.tablesaw.columns AbstractColumn)))



(defrecord simple-trigger [name p status])
; general trigger


(defrecord pool-trigger [name p status])
; trigger on pool's event

(defrecord bond-trigger [name status])
; trigger on bond's event


(defn test-trigger [trigger deal]
  "test a trigger whether get breached under context of deal, return a new trigger object with updated status"
  (let [;_ (prn "test trigger: " trigger)
        p (:p trigger)
        token (vec (vals p))]
    (m/match token
             [:pool-cumulative-default-rate op threshold _]
             (if
               (op (spv/query-deal deal [:projection :pool :total-default-percentage]) threshold)
               (assoc trigger :status true)
               (assoc trigger :status false)
               )
             [:bond-paid-off bond]
             true                                           ;(spv/query-deal deal [:])
             :else (throw (Exception.
                            (str "Trigger Pattern not match:" token
                                 "Trigger:" (:name trigger)
                                 )))
             )
    )
  )


(defn test-triggers [triggers deal]
  (do
    (loop [trg triggers d deal]
      (if-let [t-trg (first trg)]
        (recur (next trg) (test-trigger t-trg d))
        d
        ))
    )
  )

(defn run-pool-trigger [trigger pool-cf]
  "project trigger status base on pool cashflow projection"
  (let [p (:p trigger)]
    (m/match p
             {:target :pool-cumulative-default-rate :op op :threshold threshold :curable true}
             (let [trigger-flow (.select pool-cf (into-array String ["dates" "default[cumSum]"]))
                   test-level-flow (-> (.column trigger-flow 1) (.asList))]
               (map #(op % threshold) test-level-flow)
               )
             {:target :pool-cumulative-default-rate :op op :threshold-vec threshold-v :curable true}
             (let [trigger-flow (.select pool-cf (into-array String ["dates" "default[cumSum]"]))
                   trigger-vec-df (RateAssumption. "vTrigger" (u/dates (first threshold-v)) (u/ldoubles (second threshold-v)))]
               (loop [cr (.next (.iterator trigger-flow)) r []]
                 (let [n? (.hasNext cr)
                       cd (.getDate cr "dates")
                       cv (.getDouble cr "default[cumSum]")
                       test-value (.rateAt trigger-vec-df cd)]
                   (if n?
                     (recur (.next cr) (conj r (op cv test-value)))
                     (conj r (conj r (op cv test-value)))))
                 ))

             {:target :pool-cumulative-default-rate :op op :threshold-vec threshold-v}
             (let [curable-p (assoc p :curable true)
                   curable-t (assoc trigger :p curable-p)
                   curable-v (run-pool-trigger curable-t pool-cf)
                   v-len (count curable-v)
                   unbreached (take-while false? curable-v)]
               (concat unbreached (repeat (- v-len (count unbreached)) true))
               )
             {:target :pool-cumulative-default-rate :op op :threshold threshold}
             nil

             :else (repeat (.rowCount pool-cf) nil)
             )
    )
  )


(defn setup-trigger [m]
  (m/match m
           {:name n :cond cnd :status st}
           (->simple-trigger n cnd st)

           )
  )



