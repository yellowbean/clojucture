(ns clojucture.util-cashflow
  (:require [clojure.core.match :as m]
            [java-time :as jt]
            [clojucture.util :as u]
            )
  (:import [tech.tablesaw.api Table Row DoubleColumn DateColumn BooleanColumn]
           [tech.tablesaw.aggregate AggregateFunction AggregateFunctions]
           [clojucture Cashflow]
           [java.time LocalDate]
           (tech.tablesaw.columns AbstractColumn))
  )


(defn -dates [x]
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

(defn gen-dates [desc]
  (m/match desc
           {:first-date sd :interval int :times n}
           (-gen-dates sd int n)
           {:first-date sd :interval int :last-date ld}
           (-gen-dates-range sd int ld)
           :else :not-match-desc
           )
  )


(defn add-columns [^Cashflow t c-list]
  (.addColumns t (into-array AbstractColumn c-list)))

(defn gen-ts [desc]
  (let [ts-name (:name desc)
        dates (gen-dates (:dates desc))
        vs (u/gen-column (:values desc))
        init-cf (Cashflow. ts-name (-dates dates))]
    (add-columns init-cf [vs])))


(defn gen-cashflow [desc]
  (let [dates (gen-dates (:dates desc))
        init-cf (Cashflow. (:name desc) (-dates dates))]
    (m/match desc
             {:name _ :dates _ :init-bal bal :principal pal}
             nil
             {:name _ :dates _ :init-bal bal}
             (add-columns init-cf
                          [(u/gen-column
                             {:name   "BALANCE" :type :double
                              :values (repeat (.rowCount init-cf) bal)})])
             {:name _ :dates _}
             init-cf
             :else nil
             )
    )
  )


(defn find-row-by-date [^Table df ^LocalDate d]
  (let [len (.rowCount df)]
    (loop [i 0]
      (if (and (< i len) (not= (.get (.column df "dates") i) d))
        (recur (inc i))
        (doto (Row. df) (.at i))
        ))))

(defn add-cashflow [^Table x ^Table y]
  "append two cashflow with same columns , return a copy of `x` "
  (let [xc (.copy x)]
    (.append xc y)))

(defn agg-cashflow [^Table x]
  "sum up a cashflow excluding balance & dates"
  (let [xc (.copy x)
        col-list (map #(.column xc %) ["balance" "dates"])
        col-rm (into-array AbstractColumn col-list)
        y (-> xc (.removeColumns col-rm))                   ; y = table without balance & dates field
        ary-agg-functions (into-array AggregateFunction [AggregateFunctions/sum])]
    (-> x
        (.summarize (.columnNames ^Table y) ary-agg-functions)
        (.apply))
    ))


(defn sum-cashflow-by-date [^Table x ^String col ^String by-col]
  "sum column `col` in cashflow and group by `by-col` "
  (if (.isEmpty x)
    x
    (-> x
        (.summarize col
                    (into-array AggregateFunction [AggregateFunctions/sum]))
        (.by (u/strings [by-col]))
        (u/remove-sum-column)
        )
    )
  )


(defn agg-cashflow-by-interval [^Table x date-list]
  "Aggregate & combine cashflow by a vector of dates"
  (let [
        date-col (.dateColumn x "dates")
        first-date (.min date-col)
        last-date (-> (.max date-col) (jt/plus (jt/days 1)))
        trancated-date-list (filterv #(and (.isBefore % last-date) (.isAfter % first-date)) date-list)
        date-intervals (u/gen-dates-interval (cons first-date (conj trancated-date-list last-date)))

        sel-list (map #(.isBetweenIncluding date-col (first %) (second %)) date-intervals)

        split-cf-by-interval (map #(.where x %) sel-list)
        agg-cashflow-list (map #(agg-cashflow %) split-cf-by-interval)
        starting-dates (u/gen-column {:name "starting-date" :type :date :values (map first date-intervals)})
        ending-dates (u/gen-column {:name "ending-date" :type :date :values (map second date-intervals)})
        combined-cashflow (reduce add-cashflow agg-cashflow-list)]

    (-> ^Table combined-cashflow
        (.addColumns ^"[Ltech.tablesaw.columns.AbstractColumn;" (into-array AbstractColumn [starting-dates ending-dates]))
        (u/remove-sum-column))))

(defn drop-rows-if-empty [^Table x]
  "Dates with no cashflow will be dropped"
  (let [non-date-columns (filter #(not (instance? DateColumn %)) (.columns x))
        non-missing-columns-selection (map #(.isNotMissing %) non-date-columns)
        non-missing-union-selection (reduce #(.or %1 %2) non-missing-columns-selection)
        non-zero-columns-selection (map #(.isNotIn % (double-array [0.0])) non-date-columns)
        non-zero-union-selection (reduce #(.or %1 %2) non-zero-columns-selection)
        result-selection (.and non-missing-union-selection non-zero-union-selection)

        ]
    (.where x result-selection)))

(defn sub-cashflow [^Table x op ^LocalDate d]
  "select a subset of cashflow dataframe by `dates` column"
  (let [date-col (.column x "dates")]
    (->> (case op
           :>= (.isOnOrAfter date-col d)
           :> (.isAfter date-col d)
           :<= (.isOnOrBefore date-col d)
           :< (.isBefore date-col d)
           := (.isEqualTo date-col d)
           :else nil)
         (.where x)
         )))



(defn gen-end-balance
  "Generate ending balance vector given input of principal flow and initial balance"
  [^"[D" ary-prin ^Double init_balance]
  (let [ary-prin-size (alength ary-prin)
        first-remain-bal (->> (aget ary-prin 0) (- init_balance))
        ary-bal (double-array ary-prin-size first-remain-bal)]
    (doseq [i (range 1 ary-prin-size)]
      (aset-double ary-bal
                   i
                   (- (aget ary-bal (dec i)) (aget ary-prin i))))
    ary-bal))

(defn gen-beg-balance
  "Generate begining balance vector given input of principal flow and initial balance"
  [^"[D" ary-prin ^Double init_balance]
  (let [ary-prin-size (alength ary-prin)
        ary-bal (double-array ary-prin-size init_balance)]
    (doseq [i (range 1 ary-prin-size)]
      (aset-double ary-bal
                   i
                   (- (aget ary-bal (dec i)) (aget ary-prin (dec i)))))
    ary-bal))



(defn add-end-bal-column [^Table x ^Double init-bal]
  "Add a ending balance to a cashflow dataframe by taking in a array of principal"
  (let [prin-ary (-> (.column x "principal") (.asDoubleArray))
        bal-array (gen-end-balance prin-ary init-bal)]
    (.addColumns x
                 (into-array AbstractColumn [(DoubleColumn/create "end-balance" bal-array)]))
    ))

(defn add-beg-bal-column [^Table x ^Double init-bal]
  "Add a beginning balance to a cashflow dataframe by taking in a array of principal"
  (let [prin-ary (-> (.column x "principal") (.asDoubleArray))
        bal-array (gen-beg-balance prin-ary init-bal)]
    (.addColumns x
                 (into-array AbstractColumn [(DoubleColumn/create "begin-balance" bal-array)]))
    )
  )


