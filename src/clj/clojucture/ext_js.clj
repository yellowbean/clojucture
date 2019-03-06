(ns clojucture.ext-js

  (:import [tech.tablesaw.api Table]))

; this is for future data interaction with web application


(defprotocol serialized-to-web
  (to-json [ x ])
  )

(defn t2j [ x ]
  nil
  )


(extend Table
  serialized-to-web
  {:to-json t2j}
  )


