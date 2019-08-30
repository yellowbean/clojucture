(ns clojucture.io.json
  (:require
    [clojucture.tranche :as b ]
    [clojucture.pool :as p ]
    [clojucture.account :as acc ]
    [clojucture.expense :as exp ]
    )
  (:import [tech.tablesaw.api Table])
  (:import
    [clojucture.tranche sequence-bond equity-bond]
        )
  )


(defmethod print-method sequence-bond
  [ v ^java.io.Writer w]
  (.write w "<<-XYZ->>"))