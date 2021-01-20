(ns clojucture.util
  (:require
    [java-time :as jt]
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [clojure.core.match :as m]
    [clojure.string :as str]
    [medley.core :as ml]
    [com.rpl.specter :as s]
    ;[clojucture.util-cashflow :as cfu]
    )

  (:import [java.util Arrays]
           [java.time LocalDate]
           [java.time.temporal ChronoUnit]
           [org.apache.commons.lang3 ArrayUtils]
           [tech.tablesaw.api Table DoubleColumn DateColumn StringColumn BooleanColumn Row]
           [tech.tablesaw.columns AbstractColumn AbstractColumnType]
           [tech.tablesaw.columns.strings StringColumnType]
           [tech.tablesaw.columns.dates DateColumnType]
           [tech.tablesaw.columns.numbers DoubleColumnType]
           [tech.tablesaw.columns.booleans BooleanColumnType]
           [tech.tablesaw.aggregate AggregateFunction AggregateFunctions]
           [org.threeten.extra Temporals]
           [clojucture Cashflow DoubleFlow CashColumn BalanceColumn RateColumn]))




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
    :nil))



(defn gen-dates
  ([start step n]
   (take n (jt/iterate jt/plus start step)))
  ([start step]
   (jt/iterate jt/plus start step)))


(defn gen-dates-ary
  [start step n]
  (into-array java.time.LocalDate (gen-dates start step n)))


(declare gen-dates-range)
(defn gen-period-end-dates
  [^LocalDate start-date step ^LocalDate end-date]
  (let [all-months (gen-dates-range start-date step end-date)]
    (map #(jt/adjust % :last-day-of-month) all-months)))




(defn gen-dates-range
  ([start step]
   (jt/iterate jt/plus start step))
  ([start step end]
   (take-while (partial (complement jt/before?) end) (jt/iterate jt/plus start step)))
  ([start-date step end-date opt]
   (case opt
     :month-end
     (let [regular-dates (gen-period-end-dates start-date step end-date)]
       regular-dates)
     (let [regular-dates (gen-dates-range start-date step end-date)]
       regular-dates))))




(defn gen-dates-range-ary
  ([start step end]
   (into-array java.time.LocalDate (gen-dates-range start step end))))


(defn gen-dates-interval [dates]
  (let [interval-pairs (partition 2 1 dates)]
    (map #(list (first %) (jt/plus (second %) (jt/days -1))) interval-pairs)))


(defn cal-period-rate
  [^java.time.LocalDate s ^java.time.LocalDate e ^Double year-rate day-count]
  (let [r-365 (/ year-rate 365)
        r-360 (/ year-rate 360)
        bt-days (.between ChronoUnit/DAYS s e)
        bt-months (.between ChronoUnit/MONTHS s e)
        bt-years (.between ChronoUnit/YEARS s e)
        ]
    (case day-count
      :30_360
      (* (/ bt-days 30) (/ year-rate 12))
      :30_365
      (* (/ bt-days 30) (/ year-rate 12))
      :ACT_360
      (* bt-days r-360)
      :ACT_365
      (* bt-days r-365))))



(defn get-period-rate [^java.time.Period per ^Double year-rate day-count]
  (let [
        ds (.getDays per)
        ms (.getMonths per)
        ys (.getYears per)]
    ;nd (jt/local-date ys ms ds)

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
        (* ds (/ year-rate 365))))))


(defn filter-projection-dates [ds ^LocalDate sp-date]
  (let [dates-after-sp-date (fn [x] (if (instance? clojure.lang.PersistentVector x) (filter (partial jt/after? sp-date) x) x))]
    (reduce-kv #(assoc %1 %2 (dates-after-sp-date %3)) {} ds)
    ))







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
    (/ (jt/time-between ^LocalDate d1 ^LocalDate d2 :days) 365)))



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
        (aset-double ary-coupon-factor (inc i) rate-factor)))

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
        (aset-double ary-coupon-factor (inc i) rate-factor)))

    ary-coupon-factor))




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
      (count (drop-while #(jt/after? d %) ary-dates)))))


(defn find-first
  [f coll]
  (first (filter f coll)))

(defn find-last
  [f coll]
  (last (filter f coll)))


(defn find-first-by-func [d comp-f test-vec]
  (let [com-fun (partial (complement comp-f) d)]
    (find-first com-fun test-vec)))


(defn find-first-before-by-func [d comp-f test-vec]
  (let [com-fun (partial comp-f d)]
    (find-last com-fun test-vec)))




(defn find-first-date [d date-vector cmp]
  (case cmp
    :after (find-first-by-func d jt/after? date-vector)
    :before (find-first-before-by-func d jt/after? date-vector)))



(defn find-first-in-vec [d data-vector field comp-f cmp]
  (let [f (fn [x] ((partial comp-f d) (field x)))]
    (case cmp
      :after (first (filter f data-vector))
      :before (last (filter (complement f) data-vector)))))




(defn gen-float-period-rates [^"[Ljava.time.LocalDate;" ary-dates curve-to-use]
  "input: dates vector & index info, output: a array of rates should use"
  (let [ary-dates-size (alength ^"[Ljava.time.LocalDate;" ary-dates)
        ary-period-rates (double-array ary-dates-size 0)
        indexes (conj
                  (mapv #(find-first-after-index (first %) ^"[Ljava.time.LocalDate;" ary-dates) curve-to-use)
                  ary-dates-size)
        indexes-interval (partition 2 1 indexes)
        rng-vals (map vector indexes-interval curve-to-use)]

    (doseq [[[s-index end-index] [d r]] rng-vals]
      (Arrays/fill ary-period-rates ^Integer s-index ^Integer end-index ^Double r))
    ary-period-rates))


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
      (throw (Exception. "Total terms is not a multiplier of Interest Pay Frequency is not ")))

    (doseq [[s e] sum-intervals]
      (aset-double ary-int-due
                   (dec e)
                   (reduce + (ArrayUtils/subarray ary-accrued ^Integer s ^Integer e))))
    ary-int-due))



(defn new-empty-column
  ([^String n ^AbstractColumnType t ^Integer s]
   (let [c (new-empty-column n t)]
     (dotimes [_ s]
       (.appendMissing ^AbstractColumn c))
     c))
  ([^String n ^AbstractColumnType t]
   (condp instance? t
     DoubleColumnType (DoubleColumn/create ^String n)
     BooleanColumnType (BooleanColumn/create ^String n)
     StringColumnType (StringColumn/create ^String n)
     DateColumnType (DateColumn/create ^String n))))



(defn init-column [t n]
  (let [cn (name n)]
    (case t
      :double (DoubleColumn/create cn)
      :boolean (BooleanColumn/create cn)
      :string (StringColumn/create cn)
      :date (DateColumn/create cn))))



(defn init-table [n col]
  (let [tb (Table/create n)
        cols (map #(init-column (first %) (second %)) col)
        col-array (into-array AbstractColumn cols)]

    (.addColumns ^Table tb ^"[Ltech.tablesaw.columns.AbstractColumn;" col-array)))

(declare dates)
(defn gen-dflow
  "generate a time series flow with dates and values in pair"
  [name ds vs]
  (DoubleFlow. name (dates ds) (double-array vs)))

(declare gen-column)
(defn gen-cashflow
  "Create a cashflow table"
  [name columns]
  (let [t (Cashflow. name)
        column-list (map #(gen-column %) columns)
        flow_array (into-array AbstractColumn column-list)]
    (.addColumns ^Cashflow t ^"[Ltech.tablesaw.columns.AbstractColumn;" flow_array)))


(defn trans-kv-col
  "read a key/value and return a map describe it as a column"
  [k v]
  (let [t (condp = (type (first v))
            java.lang.Long :double
            java.lang.Double :double
            java.lang.Integer :double
            java.time.LocalDate :date)]
    {:name k :type t :values v})
  )


(defn gen-table
  "Create a table with columns and name"
  ([x]                                                      ;table with empty columns
   (m/match x
            (x :guard list?)
            (let [t (Table/create "EMPTY")
                  cols (map #(new-empty-column (first %) (second %)) x)]
              (.addColumns ^Table t ^"[Ltech.tablesaw.columns.AbstractColumn;" (into-array AbstractColumn cols)))

            {:name n}
            (let [col-map (seq (dissoc x :name))
                  col-list (map #(trans-kv-col (first %) (second %)) col-map)]
              (gen-table n col-list))
            :else (throw (Exception. "not-match-table"))
            )
   )
  ([^String name columns]                                   ;table with columns were described in map
   (let [t (Table/create name)
         column-list (map #(gen-column %) columns)
         flow_array (into-array AbstractColumn column-list)]
     (.addColumns ^Table t ^"[Ltech.tablesaw.columns.AbstractColumn;" flow_array))))

(declare ldoubles)
(declare dates)
(defn gen-column [desc]
  (let [column-name (name (:name desc))]
    (m/match desc
             {:type :double :values v}
             (DoubleColumn/create column-name (double-array v))
             {:type :cash :values v}
             (CashColumn. column-name (ldoubles v))
             {:type :balance :values v}
             (BalanceColumn. column-name (ldoubles v))
             {:type :rate :values v}
             (RateColumn. column-name (ldoubles v))
             {:type :date :values v}
             (DateColumn/create column-name (dates v))
             {:type :string :values v}
             (StringColumn/create column-name (into-array String v))
             {:type :bool :values v}
             (BooleanColumn/create column-name (boolean-array v))
             :else (throw (Exception. "Error in generating Column"))
             )))

(defn union-column
  [^Table x ^Table y]
  (let [x-col-names (set (.columnNames x))
        y-col-names (set (.columnNames y))
        col-types-keep (map #(.type (.column ^Table x ^String %)) x-col-names)
        col-pairs-keep (map vector x-col-names col-types-keep)
        col-names-add (vec (clojure.set/difference y-col-names x-col-names))
        col-types-add (map #(.type (.column ^Table y ^String %)) col-names-add)
        col-pairs-add (map vector col-names-add col-types-add)]

    (concat col-pairs-keep col-pairs-add)))




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
        y-col-add-ary (into-array AbstractColumn (map #(new-empty-column (first %) (second %) y-size) y-col-add))]

    [
     (.addColumns x ^"[Ltech.tablesaw.columns.AbstractColumn;" x-col-add-ary)
     (.addColumns y ^"[Ltech.tablesaw.columns.AbstractColumn;" y-col-add-ary)]))







(defn remove-sum-column [^Table t]
  (let [columns-list (.columns t)
        column-num (count columns-list)]
    (doseq [i (range column-num)]
      (let [current-col (.column t ^Integer i)
            col-name (.name ^AbstractColumn current-col)]
        (if-let [m-result (second (re-matches #".*\[(\S+)\].*" col-name))]
          (.setName current-col m-result)))))
  t)

(defn combine-cashflow
  " 'Plus' 2 cashflow tables with same column names,return combined table"
  ([^"[Ltech.tablesaw.columns.AbstractColumn;" cols ^Cashflow x ^Cashflow y]
   (let [combined-cashflow (.aggByDates x y)
         col-to-add (filter #(not= % "dates") (.columnNames combined-cashflow))
         summary-table (.summarize combined-cashflow
                                   col-to-add
                                   (into-array AggregateFunction [AggregateFunctions/sum]))
         sorted-by-dates-table (.by summary-table (into-array String ["dates"]))]
     sorted-by-dates-table))

  ([^Cashflow x ^Cashflow y]
   (let [[x-complete y-complete] (add-missing-col x y)
         common-cols (.columnNames x)]
     (combine-cashflow common-cols x-complete y-complete))))



(comment
  ;Table summary = table.summarize("sales", mean, sum, min, max).by("province", "status");
  (defn trancate-date [x u]
    (let [y (.getYear x)
          m (.getMonth x)
          d (.getDayOfMonth x)]
      (case u
        :year (jt/local-date y 1 1)
        :month (jt/local-date y m 1)
        :day x)))






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
      nil)))


(def constant
  {:periodicity
   {"P1M" (jt/months 1) "P2M" (jt/months 2) "P3M" (jt/months 3) "P6M" (jt/months 6)
    "P1Y" (jt/years 1) "P2Y" (jt/years 2)}})




(defn -cal-due-interest
  ([^Double balance ^LocalDate start-d ^LocalDate end-d day-count ^Double rate]
   (let [int-due-rate (cal-period-rate start-d end-d rate day-count)]
     (* balance int-due-rate)))
  ([balance start-d end-d day-count rate arrears]
   (+
     (-cal-due-interest balance start-d end-d day-count rate)
     arrears))
  )

(defn build-map-from-field [field list-of-maps]
  (loop [lm list-of-maps r {}]
    (if-let [this-m (first lm)]
      (recur
        (next lm)
        (assoc r (:name this-m) this-m))

      r)))

(defn dates [x]
  (into-array LocalDate x))

(defn ldoubles [x]
  (into-array Double x))

(defn strings [x]
  (into-array String x))

(defn columns [x]
  (into-array AbstractColumn x))



(defn calc-pro-rata [x y-list]
  "x -> total payment ; y-list -> a list of amounts to pay"
  (let [due-sum (reduce + y-list)
        pct-list (map #(/ % due-sum) y-list)]
    (if (>= x due-sum)
      y-list
      (map #(* x %) pct-list))
    ))

(defn out-lists [dest-file & o-list]
  "for debugging purpuse only"
  (let [max-rows (apply max (map count o-list))
        ; _ (println o-list)
        ]
    (with-open [o (io/writer dest-file)]
      (doseq [i (range 0 max-rows)]
        (->>
          (str i "," (str/join "," (map #(nth % i nil) o-list)) "\r\n")
          (.write o)))
      )
    ))

(defn list-to-map-by [l f]
  "convert a list of maps into a single map with a key from maps"
  (-> (fn [m e]
        (assoc m (f e) e))
      (reduce {} l)))

(defn list-to-map-by-info [l f]
  "convert a list of maps into a single map with a key nested in `info` "
  (-> (fn [m e]
        (assoc m (get-in e [:info f]) e))
      (reduce {} l)))



(defn stmts-to-ts [^String n stmts]
  "generate a time-series dataframe from statements "
  (let [cash (s/select [s/ALL :amount] stmts)
        ds (s/select [s/ALL :date] stmts)
        ]
    (gen-table n [
                  {:name :cash :type :cash :values cash}
                  {:name :date :type :date :values ds}])
    )
  )

(defn stmts-to-df [^String n stmts]
  (let [ds (s/select [s/ALL :date] stmts)
        fs (s/select [s/ALL :from s/NAME] stmts)
        ts (s/select [s/ALL :to s/NAME] stmts)
        amts (s/select [s/ALL :amount] stmts)
        ; :info field in stmt structure is not populated yet
        ]                                                   ;ds
    (gen-table n [{:name :date :type :date :values ds}
                  {:name :from :type :string :values fs}
                  {:name :to :type :string :values ts}
                  {:name :amount :type :double :values amts}
                  ])))

(defprotocol ICol-to-str
  (cstr [x])                                                ;
  )

(extend-type DateColumn
  ICol-to-str
  (cstr [x] (map #(.toString %) (.asList x))))

(extend-type DoubleColumn
  ICol-to-str
  (cstr [x] (map #(format "%.2f" %) (.asList x))))

(extend-type StringColumn
  ICol-to-str
  (cstr [x] (.asList x)))



(defn- prefix-col-name
  "prefix a column's name with table name"
  ([^Table x ^String c]
   (let [tn (.name x)
         nc (.setName (.column x c) (str tn "." c))]
     x))

  ([^Table x]
   (let [t-cols-names (remove #{"date"} (.columnNames x))
         nc (doseq [t-c-n t-cols-names] (prefix-col-name x t-c-n))]
     x)))

(defn- keep-col-in-ts [^Table x ^String c]
  "drop all columns in a time-series but remain only one column"
  (->
    (.retainColumns x (strings [c "date"]))
    (prefix-col-name c)))

(defn reduce-ts [^Table x ^Table y ^String col]
  "a reduce function to join two table with 'col' column "
  (let [yr (prefix-col-name y col)]
    (.fullOuter
      (.join x (strings ["date"]))
      (into-array Table [yr]))))


(defn merge-df-list-by-column [t-list ^String col]
  "a consolidate function to join a list of table by certain column `col` "
  (let [named-t-list (map #(prefix-col-name %) t-list)
        join-by-date (fn [x y]
                       (.fullOuter
                         (.join x (strings ["date"]))
                         (into-array Table [y]))
                       )]
    (reduce join-by-date named-t-list)
    ))

(defn dump-deal-html [x output-path]
  "For debug Purpose: Dump deal into a viewable html "
  (let [bond-list (s/select [:projection :bond s/MAP-VALS] x)
        output ""
        ]
    (str output
         ;     (for [b bond-list]
         ;       (.print (stmts-to-df (get b :stmts))))
         )
    ))
