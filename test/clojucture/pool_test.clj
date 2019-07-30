(ns clojucture.pool-test
  (:require
    [java-time :as jt]
    [clojure.test :refer :all]
    [clojucture.asset :as asset]
    [clojucture.assumption :as a]
    [clojucture.account :as acc ]
    [clojucture.pool :as p]
    [clojucture.util :as u]
    [clojucture.cashflow-test :as cf-t]
    [clojucture.account-test :as ac-t]
    [clojucture.asset-test :as at-t])
    
  (:import [tech.tablesaw.api Row]
           [java.time LocalDate]
           [clojucture RateAssumption]))
  

(def test-pool
  (p/->pool [ at-t/test-mortgage] (jt/local-date 2018 1 1 )))

(def t-account-1 (acc/->account :acc1 :prin 1000 []) )
(def t-account-2 (acc/->account :acc2 :int 0 []) )

(deftest pool-agg-test
  (let [ pool-assump {:prepayment (a/gen-pool-assump-df :cpr [ 0] [(jt/local-date 2014 5 5) (jt/local-date 2019 12 5)])
                      :default (a/gen-pool-assump-df :cdr [ 0] [(jt/local-date 2014 5 5) (jt/local-date 2019 12 5)])}
                      
        ;pool-agg (.collect-cashflow test-pool pool-assump [(jt/local-date 2015 3 1) (jt/local-date 2017 9 1)])
        ]
    (println "Pool Assump" pool-assump)
    ; (println (.project-cashflow test-pool pool-assump))
    ;(println "pool agg" pool-agg)
	
	))
    
  
    
  

(deftest deposit-from-pool
  (let [ pool-cf  cf-t/sample-cf
        pool-cf-collect (.aggregateByInterval pool-cf "" (into-array LocalDate [ (jt/local-date 2018 2 1) (jt/local-date 2018 6 1)]))
        accs   {:acc1 t-account-1 :acc2 t-account-2}
        mp {:principal :acc1 :interest :acc2}
        deposit-date (jt/local-date 2018 2 2)
        _ (println pool-cf-collect)
        accs-result (p/deposit-to-accs pool-cf-collect accs mp deposit-date)
        accs-int (:acc2 accs-result)
        accs-prin (:acc1 accs-result)]))
        
    ;(println (keys accs-result))
    ;(is (= 3400.0 (:balance accs-prin)))
    ;(is (= 12 (count (:stmts accs-prin))))

    ;(is (= 660.0 (:balance accs-int)))
    ;(is (= 12 (count (:stmts accs-int))))
    
(deftest tPoolCum
  (let [ pool-cf cf-t/sample-cf
        pool-cf-cum (p/calc-cumulative-amount pool-cf "interest" )

        cum-column (.column pool-cf-cum "interest[cumSum]")
        ]
    (is (= (.getDouble cum-column 0) 0.0))
    (is (= (.getDouble cum-column 1) 10.0))
    (is (= (.getDouble cum-column 11) 660.0))
    ))
