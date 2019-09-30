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
  

(def test-pool (atom nil))

(def t-account-1 (acc/->account :acc1 :prin 1000 []) )
(def t-account-2 (acc/->account :acc2 :int 0 []) )
(def test-mortgage
  (asset/->mortgage
    {:start-date (jt/local-date 2014 5 5) :periodicity (jt/months 1) :term 48 :balance 20000 :period-rate 0.01}
    {:last-paid-date  nil}
    20000 0.01 48 nil))

(defn init-t-pool [ tst ]
  (reset! test-pool (p/->pool [ at-t/test-mortgage] (jt/local-date 2018 1 1 )))
  (tst)
  )


(use-fixtures :each init-t-pool )



(deftest pool-cf-test
  (let [ pool-cf  (.project-cashflow (p/->pool [ at-t/test-mortgage] (jt/local-date 2018 1 1 )) )
        dates-list (.asList (.column  pool-cf "dates")) ]
    ;(prn dates-list)
    (prn "CF of asset")
    (println (.project-cashflow test-mortgage))
    ;(println pool-cf)
    )
  )

(comment
  (deftest deposit-from-pool
    (let [pool-cf (.project-cashflow @test-pool)
          dates-list (.asList (.column pool-cf "dates"))
          pool-cf-collect (.aggregateByInterval pool-cf "" (into-array LocalDate [(jt/local-date 2018 2 1) (jt/local-date 2018 6 1)]))
          accs {:acc1 t-account-1 :acc2 t-account-2}
          mp {:principal :acc1 :interest :acc2}
          deposit-date (jt/local-date 2018 2 2)
          accs-result (p/deposit-to-accs pool-cf-collect accs mp deposit-date)
          accs-int (:acc2 accs-result)
          accs-prin (:acc1 accs-result)]))
    ;(println (keys accs-result))
    ;(is (= 3400.0 (:balance accs-prin)))
    ;(is (= 12 (count (:stmts accs-prin))))

    ;(is (= 660.0 (:balance accs-int)))
    ;(is (= 12 (count (:stmts accs-int))))
    
(deftest tPoolCum
  (let [ pool-cf  (.project-cashflow @test-pool nil)
        pool-cf-cum (p/calc-cumulative-amount pool-cf "interest" )
        cum-column (.column pool-cf-cum "interest[cumSum]")
        ]
    (is (= (.getDouble cum-column 0) 0.0))
    (is (= (.getDouble cum-column 1) 200.0))
    (is (< (- (.getDouble cum-column 11) 2014.82831679) 0.001))
    ))
  )
