(ns clojucture.reader.cn_test
  (:require
    [clojucture.reader.cn :refer :all]
    [clojucture.assumption :as assump]
    [clojucture.pool :as p]
    [clojucture.spv :as spv]
    [clojure.test :refer :all]
    [java-time :as jt]
    [clojure.java.io :as io]
    [clojucture.util :as u]))




(deftest testPy
  (let [py-model (io/resource "china/Model.xlsx")
        model (cn-load-model! (.getFile py-model))
        ;pool (get-in model [:status :pool])
        ;deal-accounts (get-in model [:info :accounts])
        cpr-assump (assump/gen-pool-assump-df :cpr [0.1] [(jt/local-date 2017 1 1) (jt/local-date 2030 1 1)])
        cdr-assump (assump/gen-pool-assump-df :cpr [0.1] [(jt/local-date 2017 1 1) (jt/local-date 2030 1 1)])
        pool-assump {:prepayment cpr-assump :default cdr-assump}
        ;p-collect-int (get-in model [:info :p-collection-intervals])
        ;pool-cf-agg (.collect-cashflow pool pool-assump p-collect-int)

        ;rules {:principal :账户P  :interest :账户I}
        ; accs-with-deposit  (p/deposit-to-accs pool-cf-agg deal-accounts rules {:delay-days 10})
        ;waterfalls (get-in model [:info :waterfall])
        ]

    ;(println (:账户I accs-with-deposit))
    ;(.project-cashflow pool pool-assump)
  ;(println waterfalls)
    ;(println pool-cf-agg)
    ;(println (get-in model [:info :dates]))
    ;(println (assoc-in model [:status :b-rest-payment-dates]
    ;                (filter #(jt/after? % update-date) (get-in deal-setup [:info :b-payment-dates]))))
    ;(let [ [b e a] (spv/run-bonds model pool-assump)]

    ;  (println a)
    ;  )
    )
  )

  


