(ns clojucture.bond
  (:require [clojucture.type :as t]
            [java-time :as jt])
  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn]
    [tech.tablesaw.columns AbstractColumn]
    [org.apache.commons.math3.complex Complex])
  )


(defrecord sequence-bond
  [ info balance rate start-date periodicity stmt opt ]
  t/Bond


  )


;(defn new-bond [ input-bond-list ]
;  (let [ r (atom []) ]
;    (doseq [ b (first input-bond-list) ]
;      (swap! r conj
;       (sequence-bond. {:name (get-in b [:attrs :name])
;                        :original-balance (get-in b [:content :name])
;                        })
    ;(swap! r conj
      ;       (account. (get-in b [:attrs :name]) nil (Float. (get-in a [:attrs :init])) [])))
;    @r
;    )))