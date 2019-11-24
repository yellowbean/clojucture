(ns clojucture.io.csv
  (:require [clojure.java.io :as io])
  (:import [tech.tablesaw.api Table ColumnType]
           [tech.tablesaw.io.csv CsvWriter CsvReader CsvReadOptions$Builder CsvReadOptions]
           [tech.tablesaw.io Source]
           (java.util Locale)))


(defn- column-type-mapping [x]
  (case x
    :double ColumnType/DOUBLE
    :boolean ColumnType/BOOLEAN
    :date ColumnType/LOCAL_DATE
    :int ColumnType/INTEGER
    :string ColumnType/STRING
    ))



(defn read-cf [csv-name field-types]
  "internal function to read csv files into Cashflow"
  (let [ f (io/as-file (io/resource csv-name))
        reader-opts (doto
                      (CsvReadOptions/builder f)
                      (.header true)
                      (.locale Locale/US)
                      (.missingValueIndicator "")
                      (.separator (char 44)) ;44 => , in ANSI
                      )
        field-types (mapv #(column-type-mapping %) field-types)
        csv-reader (CsvReader. field-types)
        ]
    (.read csv-reader (.build reader-opts) )
    ))
