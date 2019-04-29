(ns clojucture.reader.cn_test
  (:require
    [clojucture.reader.cn :refer :all]
    [clojucture.assumption :as assump]
    [clojure.test :refer :all]
    [java-time :as jt]
    [clojure.java.io :as io]
    [clojucture.util :as u]
    )
  )

(comment 
(deftest testPy
  (let [ py-model  (io/resource "china/Model.xlsx")
       model (cn-load-model (.getFile py-model))
       pool (get-in model [:status :pool] )
        cpr-assump (assump/gen-pool-assump-df :cpr [0.1 ] [(jt/local-date 2017 1 1) (jt/local-date 2030 1 1)] )
        cdr-assump (assump/gen-pool-assump-df :cpr [0.1 ] [(jt/local-date 2017 1 1) (jt/local-date 2030 1 1)] )
        pool-assump {:prepayment cpr-assump :default cdr-assump}
        asset-cfs   (map #(.project-cashflow % pool-assump) (:assets pool) )
        ]
    ;(.project-cashflow pool pool-assump)
    (println (second asset-cfs ))
    ;(println (reduce #(.add %1 %2) asset-cfs ))
;    (u/combine-cashflow cf1 cf2)
    )
  )

)