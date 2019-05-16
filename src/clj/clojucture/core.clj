(ns clojucture.core
  (:import
    [java.time LocalDate])
  (:gen-class)
  )

(defrecord stmt [ ^LocalDate date from to ^Double amount info ]

  )

(defn -main [ & args]
  (println "reading from input"))

