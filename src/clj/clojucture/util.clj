(ns clojucture.util
  (:require
    [java-time :as jt]
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [clojure.core.match :as m]
    [medley.core :as ml]
    )
  (:import [java.util Arrays]
           [java.time LocalDate]
           [java.time.temporal ChronoUnit]
           [org.apache.commons.lang3 ArrayUtils]
           [tech.tablesaw.api Table DoubleColumn DateColumn StringColumn BooleanColumn]
           [tech.tablesaw.columns AbstractColumn AbstractColumnType]
           [tech.tablesaw.columns.strings StringColumnType]
           [tech.tablesaw.columns.dates DateColumnType]
           [tech.tablesaw.columns.numbers DoubleColumnType]
           [tech.tablesaw.columns.booleans BooleanColumnType]
           [tech.tablesaw.aggregate Summarizer AggregateFunction AggregateFunctions]
           [org.threeten.extra Temporals]
           [clojucture Cashflow]
           )

  )

(defn previous-n-workday [x n]
  (loop [d x i n]
    (if (= i 0)
      d
      (recur (.with d (Temporals/previousWorkingDay)) (dec i)))))

(defn next-n-workday [x n]
  (loop [d x i n]
    (if (= i 0)
      d
      (recur (.with d (Temporals/nextWorkingDay)) (dec i)))))



(defn get-date [x k v]
  (case k
    :next-month-day (-> (jt/plus x (jt/months 1)) (.withDayOfMonth v))
    :next-month-first-day (-> (jt/plus x (jt/months 1)) (jt/adjust :first-in-month))
    :previous-workday ((.with x (Temporals/previousWorkingDay)))
    :nil
    )
  )

(defn gen-dates
  ([start step n]
   (take n (jt/iterate jt/plus start step)))
  ([start step]
   (jt/iterate jt/plus start step))
  )

(defn gen-dates-ary
  [start step n]
  (into-array java.time.LocalDate (gen-dates start step n)))


(declare gen-dates-range)
(defn gen-period-end-dates
  [^LocalDate start-date step ^LocalDate end-date]
  (let [all-months (gen-dates-range start-date step end-date)]
    (map #(jt/adjust % :last-day-of-month) all-months)
    )
  )


(defn gen-dates-range
  ([start step]
   (jt/iterate jt/plus start step))
  ([start step end]
   (take-while (partial jt/after? end) (jt/iterate jt/plus start step)))
  ([start-date step end-date opt]
   (case opt
     :month-end
     (let [regular-dates (gen-period-end-dates start-date step end-date)]
       regular-dates)
     (let [regular-dates (gen-dates-range start-date step end-date)]
       regular-dates))
    )
  )


(defn gen-dates-range-ary
  ([start step end]
   (into-array java.time.LocalDate (gen-dates-range start step end)))
  )

(defn gen-dates-interval [dates]
  (let [interval-pairs (partition 2 1 dates)]
    (map #(list (first %) (jt/plus (second %) (jt/days -1))) interval-pairs))
  )

(defn cal-period-rate
  [^java.time.LocalDate s ^java.time.LocalDate e ^Double year-rate day-count]
  (let [r-365 (/ year-rate 365)
        r-360 (/ year-rate 360)
        bt-days (.between ChronoUnit/DAYS s e)
        bt-months (.between ChronoUnit/MONTHS s e)
        bt-years (.between ChronoUnit/YEARS s e)]
    (case day-count
      :30_360
      (* (/ bt-days 30) (/ year-rate 12))
      :30_365
      (* (/ bt-days 30) (/ year-rate 12))
      :ACT_360
      (* bt-days r-360)
      :ACT_365
      (* bt-days r-365))
    ))


(defn get-period-rate [^java.time.Period per ^Double year-rate day-count]
  (let [
        ds (.getDays per)
        ms (.getMonths per)
        ys (.getYears per)
        ;nd (jt/local-date ys ms ds)
        ]
    (case day-count
      :30_360
      (+
        (* ys year-rate)
        (* ms (/ year-rate 12))
        (* ds (/ year-rate 360)))
      :30_365
      (+
        (* ys year-rate)
        (* ms (/ year-rate 12))
        (* ds (/ year-rate 365)))
      :ACT_360
      (+
        (* ys year-rate)
        (* ms (/ year-rate 12))
        (* ds (/ year-rate 360)))
      :ACT_365
      (+
        (* ys year-rate)
        (* ms (/ year-rate 12))
        (* ds (/ year-rate 365)))
      )
    )
  )



(defn period-pmt
  [balance n-per period-rate]
  (let [c (Math/pow (+ 1 period-rate) n-per)
        a (/ (* period-rate c) (- c 1))]
    (* a balance)))

(defn gen-balance
  "Generate balance vector given input of principal flow and initial balance"
  [^"[D" ary-prin ^Double init_balance]
  (let [ary-prin-size (alength ary-prin)
        ary-bal (double-array ary-prin-size init_balance)]
    (doseq [i (range 1 ary-prin-size)]
      (aset-double ary-bal
                   i
                   (- (aget ary-bal (dec i)) (aget ary-prin i))))
    ary-bal))

(defn cal-mid-between-dates
  [^LocalDate d1 ^LocalDate d2 ^LocalDate b]
  (let [days-0 (jt/time-between d1 b :days)
        days-1 (jt/time-between d1 d2 :days)]
    (/ days-0 days-1)))

(defn cal-factor
  [^LocalDate d1 ^LocalDate d2 day-count]
  (case day-count
    :30_360
    (let [days_year (* 360 (jt/time-between ^LocalDate d1 ^LocalDate d2 :years))
          days_month (* 30 (jt/time-between ^LocalDate d1 ^LocalDate d2 :months))
          days (- (.getDayOfMonth ^LocalDate d2) (.getDayOfMonth ^LocalDate d1))]
      (/ (+ days_year days_month days) 360))
    :30_365
    (let [days_year (* 365 (jt/time-between ^LocalDate d1 ^LocalDate d2 :years)) ;(* 360 (- (.getYear  d2) (.getYear  d1)))
          days_month (* 30 (jt/time-between ^LocalDate d1 ^LocalDate d2 :months)) ;(* 30 (- (.getMonth  d2) (.getMonth  d1)))
          days (- (.getDayOfMonth ^LocalDate d2) (.getDayOfMonth ^LocalDate d1))]
      (/ (+ days_year days_month days) 365))
    :ACT_360
    (/ (jt/time-between ^LocalDate d1 ^LocalDate d2 :days) 360)
    :ACT_365
    (/ (jt/time-between ^LocalDate d1 ^LocalDate d2 :days) 365)
    )
  )

(defn gen-coupon-factor
  [^Double annual-rate ^"[Ljava.time.LocalDate;" ary-dates day-count]
  (let [dates-size (alength ^"[Ljava.time.LocalDate;" ary-dates)
        ary-coupon-factor (double-array dates-size 0)]
    (doseq [i (range (dec dates-size))]
      (let [rate-factor (* (cal-factor
                             (aget ^"[Ljava.time.LocalDate;" ary-dates i)
                             (aget ^"[Ljava.time.LocalDate;" ary-dates (inc i))
                             day-count)
                           annual-rate)]
        (aset-double ary-coupon-factor (inc i) rate-factor)
        ))
    ary-coupon-factor))

(defn gen-vector-coupon-factor
  [^doubles ary-rate ^"[Ljava.time.LocalDate;" ary-dates day-count]
  (let [dates-size (alength ^"[Ljava.time.LocalDate;" ary-dates)
        ary-coupon-factor (double-array dates-size 0)]
    (doseq [i (range (dec dates-size))]
      (let [rate-factor (* (cal-factor
                             (aget ^"[Ljava.time.LocalDate;" ary-dates i)
                             (aget ^"[Ljava.time.LocalDate;" ary-dates (inc i))
                             day-count)
                           (aget ary-rate i))]
        (aset-double ary-coupon-factor (inc i) rate-factor)
        ))
    ary-coupon-factor)
  )



(defn gen-accrued-interest
  "Generate accrued interest "
  [^doubles ary-balance ^"[Ljava.time.LocalDate;" ary-dates ^Double annual-rate day-count]
  (let [ary-balance-size (alength ary-balance)
        ary-accrued (double-array ary-balance-size 0)
        ary-coupon-factor (gen-coupon-factor annual-rate ^"[Ljava.time.LocalDate;" ary-dates day-count)]
    (doseq [i (range 1 ary-balance-size)]
      (aset-double ary-accrued
                   i
                   (* (aget ary-balance (dec i)) (aget ^doubles ary-coupon-factor i))))
    ary-accrued))


(defn find-first-after-index [^LocalDate d ^"[Ljava.time.LocalDate;" ary-dates]
  (let [date-size (alength ary-dates)]
    (-
      date-size
      (count (drop-while #(jt/after? d %) ary-dates)))
    ))

(defn find-first
  [f coll]
  (first (filter f coll)))

(defn find-last
  [f coll]
  (last (filter f coll)))


(defn find-first-by-func [d comp-f test-vec]
  (let [com-fun (partial (complement comp-f) d)]
    (find-first com-fun test-vec)
    ))

(defn find-first-before-by-func [d comp-f test-vec]
  (let [com-fun (partial comp-f d)]
    (find-last com-fun test-vec)
    ))



(defn find-first-date [d date-vector cmp]
  (case cmp
    :after (find-first-by-func d jt/after? date-vector)
    :before (find-first-before-by-func d jt/after? date-vector)
    )
  )

(defn find-first-in-vec [d data-vector field comp-f cmp]
  (let [f (fn [x] ((partial comp-f d) (field x)))]
    (case cmp
      :after (first (filter f data-vector))
      :before (last (filter (complement f) data-vector))
      )))



(defn gen-float-period-rates [^"[Ljava.time.LocalDate;" ary-dates curve-to-use]
  "input: dates vector & index info, output: a array of rates should use"
  (let [ary-dates-size (alength ^"[Ljava.time.LocalDate;" ary-dates)
        ary-period-rates (double-array ary-dates-size 0)
        indexes (conj
                  (mapv #(find-first-after-index (first %) ^"[Ljava.time.LocalDate;" ary-dates) curve-to-use)
                  ary-dates-size)
        indexes-interval (partition 2 1 indexes)
        rng-vals (map vector indexes-interval curve-to-use)
        ]
    (doseq [[[s-index end-index] [d r]] rng-vals]
      (Arrays/fill ary-period-rates ^Integer s-index ^Integer end-index ^Double r))
    ary-period-rates
    ))

;get-period-rate
(defn gen-vector-accrued-interest
  "Generate accrued interest vector "
  [^doubles ary-balance ^"[Ljava.time.LocalDate;" ary-dates ary-annual-period-rate day-count periodicity]
  (let [ary-balance-size (alength ary-balance)
        ary-accrued (double-array ary-balance-size 0)
        period-rate (map #(get-period-rate periodicity % day-count) (vec ary-annual-period-rate))
        ary-period-rate (double-array period-rate)
        ary-coupon-factor (gen-vector-coupon-factor ary-period-rate ^"[Ljava.time.LocalDate;" ary-dates day-count)]
    (doseq [i (range 1 ary-balance-size)]
      (aset-double ary-accrued
                   i
                   (* (aget ^doubles ary-balance (dec i)) (aget ^doubles ary-coupon-factor i))))
    ary-accrued))


(defn gen-interest
  "Generate interest vector with input of accrued interest vector and payment frequency"
  [^doubles ary-accrued opt]
  (let [{pay-freq :pay-freq :or {pay-freq 1}} opt
        ary-accrued-size (alength ary-accrued)
        pay-indexes (range 1 (inc ary-accrued-size) pay-freq)
        sum-intervals (partition 2 1 pay-indexes)
        ary-int-due (double-array ary-accrued-size 0)]
    (if (not= (mod (dec ary-accrued-size) pay-freq) 0)
      (throw (Exception. "Total terms is not a multiplier of Interest Pay Frequency is not "))
      )
    (doseq [[s e] sum-intervals]
      (aset-double ary-int-due
                   (dec e)
                   (reduce + (ArrayUtils/subarray ary-accrued ^Integer s ^Integer e))))
    ary-int-due
    ))


(defn gen-column [[k v]]
  "Create a table column with given column name and value vector"
  (try
    (case (str (.getComponentType (.getClass v)))
      "double" (DoubleColumn/create (name k) v)
      "string" (StringColumn/create (name k) ^"[Ljava.lang.String;" v)
      "boolean" (BooleanColumn/create (name k) ^booleans v)
      "class java.time.LocalDate" (DateColumn/create (name k) ^"[Ljava.time.LocalDate;" v)
      )
    (catch Exception e
      (println "Exception:" e)
      )))


(defn new-empty-column
  ([^String n ^AbstractColumnType t ^Integer s]
   (let [c (new-empty-column n t)]
     (dotimes [_ s]
       (.appendMissing ^AbstractColumn c))
     c))
  ([^String n ^AbstractColumnType t]
   (println t)
   (condp instance? t
     DoubleColumnType (DoubleColumn/create ^String n)
     BooleanColumnType (BooleanColumn/create ^String n)
     StringColumnType (StringColumn/create ^String n)
     DateColumnType (DateColumn/create ^String n)
     ))
  )

(defn init-column [t n]
  (let [cn (name n)]
    (case t
      :double (DoubleColumn/create cn)
      :boolean (BooleanColumn/create cn)
      :string (StringColumn/create cn)
      :date (DateColumn/create cn)
      ))
  )

(defn init-table [n col]
  (let [tb (Table/create n)
        cols (map #(init-column (first %) (second %)) col)
        col-array (into-array AbstractColumn cols)
        ]
    (.addColumns ^Table tb ^"[Ltech.tablesaw.columns.AbstractColumn;" col-array)
    )
  )


(defn gen-cashflow
  "Create a cashflow table"
  [name columns]
    (let [t (Cashflow. name)
         column-list (map #(gen-column %) columns)
         flow_array (into-array AbstractColumn column-list)]
     (.addColumns ^Cashflow t ^"[Ltech.tablesaw.columns.AbstractColumn;" flow_array )))

(defn gen-table
  "Create a table with columns and name"
  ([column-pairs]                                           ;table with empty columns
   (let [t (Table/create "EMPTY")
         cols (map #(new-empty-column (first %) (second %)) column-pairs)]
     (.addColumns ^Table t ^"[Ltech.tablesaw.columns.AbstractColumn;" (into-array AbstractColumn cols))))
  ([name columns]                                           ;table with columns were described in map
   (let [t (Table/create name)
         column-list (map #(gen-column %) columns)
         flow_array (into-array AbstractColumn column-list)]
     (.addColumns ^Table t ^"[Ltech.tablesaw.columns.AbstractColumn;" flow_array)))
  )


(defn union-column
  [^Table x ^Table y]
  (let [x-col-names (set (.columnNames x))
        y-col-names (set (.columnNames y))
        col-types-keep (map #(.type (.column ^Table x ^String %)) x-col-names)
        col-pairs-keep (map vector x-col-names col-types-keep)
        col-names-add (vec (clojure.set/difference y-col-names x-col-names))
        col-types-add (map #(.type (.column ^Table y ^String %)) col-names-add)
        col-pairs-add (map vector col-names-add col-types-add)
        ]
    (concat col-pairs-keep col-pairs-add)
    )
  )


(defn add-missing-col [^Table x ^Table y]
  (let [
        x-col-names (set (.columnNames x))
        x-col-types (map #(.type (.column x ^String %)) x-col-names)
        x-col-pairs (set (map vector x-col-names x-col-types))

        y-col-names (set (.columnNames y))
        y-col-types (map #(.type (.column y ^String %)) y-col-names)
        y-col-pairs (set (map vector y-col-names y-col-types))

        x-col-add (vec (clojure.set/difference y-col-pairs x-col-pairs))
        y-col-add (vec (clojure.set/difference x-col-pairs y-col-pairs))

        x-size (.rowCount x)
        y-size (.rowCount y)

        x-col-add-ary (into-array AbstractColumn (map #(new-empty-column (first %) (second %) x-size) x-col-add))
        y-col-add-ary (into-array AbstractColumn (map #(new-empty-column (first %) (second %) y-size) y-col-add))
        ]
    [
     (.addColumns x ^"[Ltech.tablesaw.columns.AbstractColumn;" x-col-add-ary)
     (.addColumns y ^"[Ltech.tablesaw.columns.AbstractColumn;" y-col-add-ary)
     ]

    ))

(defn add-cashflow [^Table x ^Table y]
  "append two cashflow with same columns , return a copy of `x` "
  (let [xc (.copy x)]
    (.append xc y)
    ))

(defn remove-sum-column [^Table t]
  (let [columns-list (.columns t)
        column-num (count columns-list)]
    (doseq [i (range column-num)]
      (let [current-col (.column t ^Integer i)
            col-name (.name ^AbstractColumn current-col)]
        (if-let [m-result (second (re-matches #".*\[(\S+)\].*" col-name))]
          (.setName current-col m-result))
        )))
  t)

(defn combine-cashflow
  " 'Plus' 2 cashflow tables with same column names,return combined table"
  ([^"[Ltech.tablesaw.columns.AbstractColumn;" cols ^Cashflow x ^Cashflow y]
   (let [ combined-cashflow (.add x y)
         col-to-add (filter #(not= % "dates") (.columnNames combined-cashflow))
         ;_ (println col-to-add)
         summary-table (.summarize combined-cashflow
                                   col-to-add
                                   (into-array AggregateFunction [AggregateFunctions/sum]))
         sorted-by-dates-table (.by summary-table (into-array String ["dates"]))]
     (Cashflow. sorted-by-dates-table) ))

  ([^Cashflow x ^Cashflow y]
   (let [[x-complete y-complete] (add-missing-col x y)
         common-cols (.columnNames x)]
     (combine-cashflow common-cols x-complete y-complete)
     ))
  )

;Table summary = table.summarize("sales", mean, sum, min, max).by("province", "status");
(defn trancate-date [x u]
  (let [y (.getYear x)
        m (.getMonth x)
        d (.getDayOfMonth x)]
    (case u
      :year (jt/local-date y 1 1)
      :month (jt/local-date y m 1)
      :day x
      ))
  )


(defn agg-cashflow [^Table x]
  "sum up a cashlfow excluding balance & dates"
  (let [xc (.copy x)
        col-list (map #(.column xc %) ["balance" "dates"])
        col-rm (into-array AbstractColumn col-list)
        y (-> xc (.removeColumns col-rm))
        ary-agg-functions (into-array AggregateFunction [AggregateFunctions/sum])
        ]
    (-> x
        (.summarize (.columnNames ^Table y) ary-agg-functions)
        (.apply)
        )))



(defn agg-cashflow-by-interval [^Table x date-intervals]
  "Aggregate & combine cashflow by a vector of dates"
  (let [date-col (.dateColumn x "dates")
        sel-list (map #(.isBetweenIncluding date-col (first %) (second %)) date-intervals)
        split-cf-by-interval (map #(.where x %) sel-list)
        agg-cashflow-list (map #(agg-cashflow %) split-cf-by-interval)
        starting-dates (gen-column ["starting-date" (into-array LocalDate (for [[s _] date-intervals] s))])
        ending-dates (gen-column ["ending-date" (into-array LocalDate (for [[_ e] date-intervals] e))])
        combined-cashflow (reduce add-cashflow agg-cashflow-list)
        ]
    (-> ^Table combined-cashflow
        (.addColumns ^"[Ltech.tablesaw.columns.AbstractColumn;" (into-array AbstractColumn [starting-dates ending-dates]))
        (remove-sum-column)
        )
    ))


(defn load-interest-rate [p]
  (let [x (slurp (io/resource p))]
    (json/read-str x :key-fn keyword)))

(defn load-json-to-map [p]
  "load a json file into clojure map"
  (-> (slurp p)
      (json/read-str :key-fn keyword)))


(defn backout-original-balance [current-balance term-remains term-origin asset-type]
  (case asset-type
    :installments (-> (/ term-origin term-remains) (* current-balance))
    nil
    ))

(def constant
  {:periodicity
   {"P1M" (jt/months 1) "P2M" (jt/months 2) "P3M" (jt/months 3) "P6M" (jt/months 6)
    "P1Y" (jt/years 1) "P2Y" (jt/years 2)
    }
   }
  )

(defn -cal-due-interest
  ([balance start-d end-d day-count rate]
   (let [int-due-rate (cal-period-rate start-d end-d rate day-count)]
     (* balance int-due-rate)))
  ([balance start-d end-d day-count rate arrears]
   (+
     (-cal-due-interest balance start-d end-d day-count rate)
     arrears)))

(defn build-map-from-field [ field list-of-maps ]
  (loop [ lm list-of-maps  r {}]
    (if-let [  this-m (first lm)  ]
      (recur
        (next lm)
        (assoc r (:name this-m) this-m)
        )
      r ) ) )

(defn dates [ x ]
  (into-array LocalDate x))