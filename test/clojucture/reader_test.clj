(ns clojucture.reader-test
  (:require
    [clojure.java.io :as io]
    [clojucture.asset :as asset ]
    [clojucture.reader :as rdr]
    )
  )


;(def sample-csv (io/as-file (io/resource"china\\installments.csv")))
;(def column-mapping {:start-date 1 :periodicity 2 :term 3 :balance 4 :fee-rate 5 })

;(rdr/table-to-asset sample-csv column-mapping :installments
;                    )