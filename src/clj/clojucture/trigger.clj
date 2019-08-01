(ns clojucture.trigger
  (:require [clojure.core.match :as m]
            [clojucture.util-cashflow :as cfu])
  )



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
             {:target :pool-cumulative-default-rate :op op :threshold threshold}
             (let [ trigger-flow (.select pool-cf (into-array String ["dates" "default[cumSum]"]))
                   test-level-flow (-> (.column trigger-flow 1) (.asList))
                   test-result (map #(op % threshold) test-level-flow)
                   ]
                 test-result
               )
             :else :not-match-trigger
             )
    )
  )



(defrecord bond-trigger [name status])                      ; trigger on bond's event


