(ns clojucture.assumption
  (:require [java-time :as jt]
            [clojure.core.match :as m]
            [clojucture.util :as u])
  (:import [java.time LocalDate]
           [clojucture DoubleFlow]
           [clojucture RateAssumption]))



(defn pick-rate-by-date [^LocalDate d index-curve]
  (let [shifted-curve (for [[d r] index-curve] [(jt/plus d (jt/days -1)) r])]
    (second (last (filter #(jt/after? d (first %)) shifted-curve)))))


(defn apply-curve [curves float-info]
  (let [current-curve ((:index float-info) curves)
        reset-dates (:reset float-info)
        index-rate-at-dates (loop [d reset-dates r []]
                              (if (nil? d)
                                r
                                (recur (next d)
                                       (conj r (pick-rate-by-date (first d) current-curve)))))]


    (m/match float-info
             {:margin mg}
             (map #(+ mg %) index-rate-at-dates)
             {:factor ft}
             (map #(* ft %) index-rate-at-dates)
             :else nil)))




(defn setup-curve [index dates rates]
  (let [pairs (map vector dates rates)
        p (sort-by first jt/before? pairs)]
    {index p}))

(defn curve-to-df [n ps]
  (let [dates (into-array LocalDate (map first ps))
        rs (double-array (map second ps))]
    (DoubleFlow. (name n) dates rs)))

(defn smm2cpr [^Double smm]
  (- 1 (Math/pow (- 1 smm) 12)))

(defn cpr2d [^Double cpr]
  "convert CPR to daily rate"
  (- 1 (Math/pow (- 1 cpr) 1/365)))

(defn d2cpr [^Double day-rate]
  (- 1 (Math/pow (- 1 day-rate) 365)))

(defn cpr2smm [^Double cpr]
  (- 1 (Math/pow (- 1 cpr) 1/12)))

(defn cdr2smm [^Double cdr]
  (cpr2smm cdr))

(defn smm2cdr [^Double smm]
  (smm2cpr smm))

(defn gen-pool-assump-df [curve-type v observe-dates]
  " v -> a vector list ; observe-dates -> a list of LocalDates"
  (let [ds (u/dates observe-dates)]
    (as->
      (m/match [curve-type v]
               ;[:smm (v :guard #(vector? %))]
               ;(map #(- 1 (Math/pow (- 1 (second %)) (first %))) factors-m)
               [(:or :cpr :cdr) (v :guard #(seqable? %))]
               (map cpr2d v)
               [(:or :cpr :cdr) _ ]
               [(cpr2d v)]
               :else nil) rs
      (RateAssumption. (name curve-type) ds (u/ldoubles rs)))))


(defn gen-asset-assump
  "convert a pool assumption to asset level assumption"
  [^RateAssumption pool-assumption observe-dates]
  (let [obs-ary (u/dates observe-dates)]
    (if (= (alength obs-ary) 0)
      nil
      (.apply pool-assumption obs-ary))))


(defn complete-assump [x]
  ""
  (cond-> x
          (not (contains? x :default)) (assoc :default nil)
          (not (contains? x :prepayment)) (assoc :prepayment nil)
          (not (contains? x :recovery-lag)) (assoc :recovery-lag 0)
          (not (contains? x :recovery-rate)) (assoc :recovery-rate 0)))


(defn gen-assump-curve [ ds assump ]
  ; remain dates -> a list of dates;
  ; assumption a map with key ":prepayment  :default"
  ;                       value : RateAssumption record with range of dates and doubles
  "convert a pool level assumption to asset level"
  (let [ppy-curve (:prepayment assump) ; table type
        def-curve (:default assump)  ; table type
        dsa (u/dates ds)
        apply-rate-fn (fn [ x ]
                        (if (nil? x)
                          (double-array (count ds) 0.0)
                          (.apply x dsa) ) )
        ]
    {
     :prepayment-curve (apply-rate-fn ppy-curve)
     :default-curve    (apply-rate-fn def-curve)
     :recovery-curve   :nil
     :recover-lag      :nil}))

(defn -build-assump [ x ]
  "dispatch to different assumption type "
  (m/match x
           {:name :prepayment :type tpe :dates ds :values vs}
           (gen-pool-assump-df tpe vs ds)
           {:name :default :type tpe :dates ds :values vs}
           (gen-pool-assump-df tpe vs ds)
           :else :not-match-assmp
           )

  )

(defn build [ d ]
  "take a assumption list and return a map of assumption in form of records"
  (loop [r {} input-assump-list (vals d)]
    (if-let [assump-item (first input-assump-list)]
      (recur (assoc r (:name assump-item) (-build-assump assump-item)) (next input-assump-list))
      r
      )
    )

  )
