(ns clojucture.local.nowhere_test
  (:require [clojure.test :refer :all]
            [clojucture.local.nowhere :as nw]
            [clojucture.builder :as br]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            ))



(deftest tRunDeal
  (let [ d-map (edn/read-string (slurp (io/resource "basic01.txt")))
        load-d (br/load-deal d-map)
        run-d (nw/run-deal load-d nil)
        ]
    ;(get-in run-d [:projection :bond])
    ;(prn (get-in run-d [:projection ]))

    )

  )

