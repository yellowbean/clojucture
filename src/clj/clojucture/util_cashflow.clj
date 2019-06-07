(ns clojucture.util-cashflow
  (:require [clojure.core.match :as m]
            [clojucture.util :as u])
  (:import [tech.tablesaw.api Table Row DoubleColumn DateColumn BooleanColumn]
             [clojucture Cashflow]
           [java.time LocalDate])
  )




(defn gen-dates [ desc ]
  (m/match desc
    {:first-date sd :interval int :times n}
           (u/gen-dates sd int n)
    {:first-date sd :interval int :last-date ld}
           (u/gen-dates-range sd int ld)



    :else :not-match-desc
    )
  )

(defn gen-column [ desc ]
  (let [ column-name (:name desc)]
    (m/match desc
     {:type :double :values v }
           (DoubleColumn/create column-name (double-array v))
     {:type :date :values v }
           (DateColumn/create column-name (u/dates v))
     {:type :bool :values v }
           (BooleanColumn/create column-name (boolean-array v))
     :else nil
     ) ) )

(defn gen-cashflow [ desc ]
  (let [dates (gen-dates (:dates desc))
        init-cf (Cashflow. (:name desc) (u/dates dates))
        ]


    (m/match desc
             {:name _ :dates _} init-cf
             :else nil
             )
    )
  )




(defn calc-interest [ ^Cashflow t i-info ]





   )

