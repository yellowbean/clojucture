(ns clojucture.io.csv
  (:require [clojure.java.io :as io])
  (:import [tech.tablesaw.api Table ColumnType ]
           [tech.tablesaw.io.csv CsvWriter CsvWriteOptions CsvReader ] 
           [tech.tablesaw.io Source] 
           
           ) )


(defn column-type-mapping [x]
 (case x
  :double ColumnType/DOUBLE 
  :boolean ColumnType/BOOLEAN 
  :date ColumnType/LOCAL_DATE 
  :int ColumnType/INTEGER
  :string ColumnType/STRING
  ))



(defn read-cf [ csv-name field-types ]
  "internal function to read csv files into Cashflow"
    (let [s (Source. (io/as-file  (io/resource csv-name)) )
          field-types (mapv #(column-type-mapping %) field-types) 
        csv-reader (CsvReader. field-types ) ]
      (.read csv-reader s ))) 
