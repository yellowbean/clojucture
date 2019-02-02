(ns clojucture.core
  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn]
    [tech.tablesaw.columns AbstractColumn]
    [org.apache.commons.math3.complex Complex])
  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn StringColumn BooleanColumn]
    [java.time LocalDate])
  )

(defrecord stmt [ ^LocalDate date from to ^Double amount info ]

  )
