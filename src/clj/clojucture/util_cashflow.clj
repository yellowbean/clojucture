(ns clojucture.util-cashflow
  (:require [clojure.core.match :as m]
            [java-time :as jt]
            )
  (:import [tech.tablesaw.api Table Row DoubleColumn DateColumn BooleanColumn]
           [clojucture Cashflow]
           [java.time LocalDate]
           (tech.tablesaw.columns AbstractColumn))
  )


(defn -dates [ x ]
  (into-array LocalDate x))

(defn -gen-dates
  ([start step n]
   (take n (jt/iterate jt/plus start step)))
  ([start step]
   (jt/iterate jt/plus start step))
  )



(declare -gen-dates-range)
(defn -gen-period-end-dates
  [^LocalDate start-date step ^LocalDate end-date]
  (let [all-months (-gen-dates-range start-date step end-date)]
    (map #(jt/adjust % :last-day-of-month) all-months)
    )
  )

(declare -gen-dates-range)
(defn -gen-period-end-dates
  [^LocalDate start-date step ^LocalDate end-date]
  (let [all-months (-gen-dates-range start-date step end-date)]
    (map #(jt/adjust % :last-day-of-month) all-months)
    )
  )


(defn -gen-dates-range
  ([start step]
   (jt/iterate jt/plus start step))
  ([start step end]
   (take-while (partial jt/after? end) (jt/iterate jt/plus start step)))
  ([start-date step end-date opt]
   (case opt
     :month-end
     (let [regular-dates (-gen-period-end-dates start-date step end-date)]
       regular-dates)
     (let [regular-dates (-gen-dates-range start-date step end-date)]
       regular-dates))
    )
  )

(defn gen-dates [ desc ]
  (m/match desc
    {:first-date sd :interval int :times n}
           (-gen-dates sd int n)
    {:first-date sd :interval int :last-date ld}
           (-gen-dates-range sd int ld)



    :else :not-match-desc
    )
  )

(defn gen-column [ desc ]
  (let [ column-name (:name desc)]
    (m/match desc
     {:type :double :values v }
           (DoubleColumn/create column-name (double-array v))
     {:type :date :values v }
           (DateColumn/create column-name (-dates v))
     {:type :bool :values v }
           (BooleanColumn/create column-name (boolean-array v))
     :else nil
     ) ) )

(defn add-columns [ ^Cashflow t c-list ]
  (.addColumns t (into-array AbstractColumn c-list)) )

(defn gen-ts [ desc ]
  (let [ ts-name (:name desc)
        dates (gen-dates (:dates desc))
        vs (gen-column (:values desc ))
        init-cf (Cashflow. ts-name (-dates dates))
        ]
    (add-columns init-cf [vs]) ) )


(defn gen-cashflow [ desc ]
  (let [dates (gen-dates (:dates desc))
        init-cf (Cashflow. (:name desc) (-dates dates))
        ]
     (m/match desc
       {:name _ :dates _ :init-bal bal :principal pal}
             nil


       {:name _ :dates _ :init-bal bal }
         (add-columns init-cf
                  [(gen-column
                    {:name "BALANCE" :type :double
                     :values (repeat (.rowCount init-cf) bal)})])
      {:name _ :dates _ }
             init-cf
       :else nil
       )
    )
  )




(defn calc-interest [ ^Cashflow t i-info ]





   )

