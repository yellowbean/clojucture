(ns clojucture.local.nowhere_test
  (:require [clojure.test :refer :all]
            [clojucture.local.nowhere :as nw]
            [clojucture.io.html :as io-html]
            [clojucture.builder :as br]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojucture.account :as a]
            [com.rpl.specter :as s]
            [clojucture.util :as u]
            [clojucture.spv :as spv]))



(deftest tRunDeal-simple
  (let [d-map (edn/read-string (slurp (io/resource "basic01.txt")))
        load-d (br/load-deal d-map)
        ;run-d (nw/run-deal load-d nil)
        ]
    ;(io-html/deal-to-html run-d "C:\\changer\\engine\\out.html")
    )
  )

(deftest tRunDeal-trigger
  (let [source-deal-file "basic02.txt"
        d-map (edn/read-string (slurp (io/resource source-deal-file)))
        load-d (br/load-deal d-map)
        ;run-d (nw/run-deal load-d nil)
        ]
    ;(io-html/deal-to-html
    ;  run-d
    ;  (str "C:\\changer\\engine\\"  source-deal-file ".html")
    ))

