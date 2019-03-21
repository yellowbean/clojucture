(ns clojucture.io.json
  (:import [tech.tablesaw.api Table])
  (:use [ dk.ative.docjure.spreadsheet ])
  )


(defprotocol output-xls-range
  (to-xl-range [ x ])
  )

(extend-protocol output-xls-range
  Table
  (to-xl-range [ ^Table x  row col]
    (let [ column-names (.columnNames x)
          cols (.colums x)
          row-number (.rowCount x)
          ]
      (doseq [])


      )

    )
  )


;(extend Table
;  serialized-to-web
;  {:to-json t2j}
;  )
; this is for future data interaction with web application
;(defprotocol serialized-to-js
;  (to-json [ x ])
;  )
;(defn t2j [ x ]
;  nil
;  )