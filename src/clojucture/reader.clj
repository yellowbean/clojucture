(ns clojucture.reader
  (:require
    [clojucture.asset :as asset]
    [clojucture.util :as u]
    [clojure.java.io :as io])
  ;(:use [ dk.ative.docjure.spreadsheet ])
  (:import
    [tech.tablesaw.api Table Row DoubleColumn DateColumn StringColumn BooleanColumn]
    ;[tech.tablesaw.io DataFrameReader]
    [java.util Locale]
    [tech.tablesaw.io.csv CsvReader CsvReadOptions$Builder])
  )

(defn load-asset [ fp opt ]

  )


(defn reader-options [ opts ]
  (let [ b (CsvReadOptions$Builder.)]
    (cond-> b
      (contains? opts :locale ) (.locale (:locale opts))
      (contains? opts :header ) (.header (:header opts))
      (contains? opts :file ) (.file (io/as-file (:file opts)))
            true (.build ))
  )
)

(defn read-into-table-csv [ reader-opts ]
  (let [ ro (reader-options reader-opts)
        t (Table/read)]
    (.csv t ro)
    )
  )



(defn field-type-map [ ^Row r k v]
  (case k
    :start-date (.getDate r v)
    :periodicity (-> (.getString r v) ((u/constant :periodicity)))
    :term (.getInt r v )
    :balance (.getDouble r v )
    :fee-rate (.getDouble r v)
    )
  )


(defn row-to-asset [ ^Row row field-map asset-type]
  (doseq [ [ k v ] field-map]
    (println (field-type-map row k v))
    )
  )

(defn table-to-asset [ table field-map asset-type ]
  (loop [ trs (.iterator table) r [] ]
    (if-not (.hasNext trs)
      r
      (let [ current-row (.next trs)]
        (recur trs (conj r (row-to-asset current-row field-map asset-type )))
      )
    )
  ))
;(defn load-xl [ fp sh-name column-flags asset-class opt ]
;  (let [ raw-maps (->> (load-workbook fp )
;              (select-sheet sh-name)
;              (select-columns column-flags))
;         remove-first-line (fn [x]     (if (get opt :header false)
;                                         (next x)
;                                         x
;                                         ))
;         asset-of-function (fn [ a ] (case a
;                                      :installments asset/map->installments
;                                      :mortgage asset/map->mortgage
;;                                      :loan asset/map->loan
;                                      ))
;        ]
;
;    (-> raw-maps
;        (remove-first-line) ((asset-of-function asset-class)))
;    ))