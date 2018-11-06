(ns clojucture.assumption
  (:require [clojucture.type :as t]
            [java-time :as jt])
  (:import [java.time LocalDate])
  )

(defn pick-rate-by-date [ ^LocalDate d index-curve ]
  (let [shifted-curve (for [ [ d r ] index-curve] [ (jt/plus d (jt/days -1)) r ] )]
    (second (last (filter #(jt/after? d (first %)) shifted-curve))))
  )

(defn apply-curve [ curves float-info reset-dates ]
  (let [
        current-curve ((:index float-info) curves)
        index-rate-at-dates (loop [ d reset-dates r []]
                        (if (nil? d)
                          r
                          (recur (next d)
                                 (conj r (pick-rate-by-date (first d) current-curve))
                                 )))
        margin (:margin float-info)
        index-with-margin (map #(+ margin %) index-rate-at-dates)]
    {:reset-dates reset-dates :index-with-margin index-with-margin}
    ;(map vector reset-dates index-with-margin )
  ))


(defn gen-curve [index dates rates ]
  (let [ pairs (map vector dates rates ) ]
    { index pairs})
  )

;