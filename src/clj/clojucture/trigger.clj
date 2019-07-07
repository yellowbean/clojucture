(ns clojucture.trigger
  (:require [clojure.core.match :as m]
            [clojucture.util-cashflow :as cfu])

  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn]
    [java.time LocalDate]
    )
  )

(defprotocol Trigger
  (breach? [x d pd])
  )

(defn fetch-level [d i ^LocalDate payment-date]
  "Find projection variable with specific payment date"
  (m/match i
           {:watch :pool-cumulative-default-rate}
           (let [pool-df (get-in d [:projection :pool])]
             (-> (cfu/find-row-by-date pool-df payment-date)
                 (.getDouble "Cumulative Default Rate")))



           :else :not-match-trigger-target
           ))



(defrecord trigger [info op threshold]
  Trigger
  (breach? [x d payment-date]
    (let [current-level (fetch-level d info payment-date)]
      (if (op current-level threshold)
        :breached
        :unbreached
        )))
  )


