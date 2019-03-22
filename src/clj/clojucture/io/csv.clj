(ns clojucture.io.csv
  (:require [clojure.java.io :as io])
  (:import [tech.tablesaw.api Table]
           [tech.tablesaw.io.csv CsvWriter CsvWriteOptions] ) )


(defn export-table [ ^Table t dest ]
  (let [ dest-f  (io/as-file dest)
        opt (CsvWriteOptions/builder dest-f )]
  (doto
    (CsvWriter. t opt)
    (.write)))
  )