(ns clojucture.trigger
  (:require [clojure.core.match :as m]
            [clojucture.util-cashflow :as cfu]
            [clojucture.util :as u])
  (:import (clojucture RateAssumption)))



(defrecord trigger [info op threshold]
  Trigger
  (breach? [x d payment-date]
    (let [current-level (fetch-level d info payment-date)]
      (if (op current-level threshold)
        :breached
        :unbreached
        )))
  )


(defrecord pool-trigger [name p status])
; trigger on pool's event


(defn run-pool-trigger [trigger pool-cf]
  (let [p (:p trigger)]
    (m/match p
             {:target :pool-cumulative-default-rate :op op :threshold threshold :curable true}
             (let [ trigger-flow (.select pool-cf (into-array String ["dates" "default[cumSum]"]))
                   test-level-flow (-> (.column trigger-flow 1) (.asList)) ]
                 (map #(op % threshold) test-level-flow)
               )
             {:target :pool-cumulative-default-rate :op op :threshold-vec threshold-v :curable true}
             (let [trigger-flow (.select pool-cf (into-array String ["dates" "default[cumSum]"]))
                   ;start-date (.get d-column 0)
                   ;end-date (.get d-column (dec n-rows))
                   trigger-vec-df (RateAssumption. "vTrigger" (u/dates (first threshold-v)) (u/ldoubles (second threshold-v)))
                   ]
               (loop [ cr (.next (.iterator trigger-flow)) r [] ]
                 (if (.hasNext cr)
                   (let [ cd (.getDate cr "dates")
                         cv (.getDouble cr "default[cumSum]")
                         test-value (.rateAt trigger-vec-df cd)
                         ;_ (println cd cv test-value)
                         ]
                     (recur (.next cr) (conj r (op cv test-value))))
                   r )))

             {:target :pool-cumulative-default-rate :op op :threshold-vec threshold-v }
             (let [curable-p (assoc p :curable true)
                   curable-t (assoc trigger :p curable-p)
                   curable-v (run-pool-trigger curable-t pool-cf)
                   v-len (count curable-v)
                   unbreached (take-while false? curable-v) ]
               (concat unbreached (repeat (- v-len (count unbreached)) true))
               )
             {:target :pool-cumulative-default-rate :op op :threshold threshold }
             nil

             :else :not-match-trigger
             )
    )
  )



(defrecord bond-trigger [name status])                      ; trigger on bond's event


