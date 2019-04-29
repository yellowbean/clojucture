(ns clojucture.assumption
  (:require [clojucture.type :as t]
            [java-time :as jt]
            [clojure.core.match :as m]
            [clojucture.util :as u])
  (:import [java.time LocalDate]
           clojucture.DoubleFlow
           (clojucture RateAssumption)
           (org.apache.commons.math3.analysis.function Pow))
  )

(defn pick-rate-by-date [ ^LocalDate d index-curve ]
  (let [shifted-curve (for [ [ d r ] index-curve] [ (jt/plus d (jt/days -1)) r ] )]
    (second (last (filter #(jt/after? d (first %)) shifted-curve))))
  )

(defn apply-curve [ curves float-info ]
  (let [current-curve ((:index float-info) curves)
        reset-dates (:reset float-info)
        index-rate-at-dates (loop [ d reset-dates r []]
                        (if (nil? d)
                          r
                          (recur (next d)
                                 (conj r (pick-rate-by-date (first d) current-curve))
                                 )))
        ]
    (m/match float-info
      { :margin mg }
        (map #(+ mg %) index-rate-at-dates)
      { :factor ft }
        (map #(* ft %) index-rate-at-dates)
      :else nil
      )
  ))


(defn setup-curve [ index dates rates ]
  (let [ pairs (map vector dates rates )
        p (sort-by first jt/before? pairs)]
    { index p}) )

(defn curve-to-df [ n ps ]
  (let [ dates (into-array LocalDate (map first ps))
        rs (double-array (map second ps))]
    (DoubleFlow. (name n) dates rs)))

(defn smm2cpr [ ^Double smm ]
  (- 1 (Math/pow (- 1 smm) 12)))

(defn cpr2smm [ ^Double cpr ]
  (- 1 (Math/pow (- 1 cpr) 1/12)))

(defn cdr2smm [ ^Double cdr ]
  (cpr2smm cdr))

(defn smm2cdr [ ^Double smm ]
  (smm2cpr smm))


(defn gen-pool-assump-df [curve-type v observe-dates]
  (let [d-intervals (u/gen-dates-interval observe-dates)
        days-intervals (map #(jt/time-between (first %) (second %) :days) d-intervals)

        interval-start (into-array LocalDate (map first d-intervals))
        interval-end (into-array LocalDate (map second d-intervals))

        ]
    (as->
      (m/match [curve-type v]
               ;[:smm (v :guard #(vector? %))]
               ;(map #(- 1 (Math/pow (- 1 (second %)) (first %))) factors-m)
               [(:or :cpr :cdr) (v :guard #(vector? %))]
               (let [ daily-pct (map #(- 1 (Math/pow (- 1 %) (/ 1 365))) v ) ]
                 daily-pct
                 )
               :else nil) rs
      (RateAssumption. (name curve-type) interval-start interval-end (double-array rs ) )
    )
  ))

; to be verified, it is a mess here
(defn gen-asset-assump
  [^RateAssumption pool-assumption observe-dates]
  (let [obs-ary (into-array LocalDate observe-dates)]
    (seq (.project pool-assumption obs-ary)) )
  )

(defn gen-assump [ x ]
  (m/match x
           :else nil
           )
  )