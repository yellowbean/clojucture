(ns clojucture.pool-test
  (:require
    [java-time :as jt]
    [clojure.test :refer :all]
    [clojucture.asset :as asset ]
    [clojucture.pool :as p]
    [clojucture.util :as u]
    [clojucture.cashflow-test :as cf-t]
    [clojucture.account-test :as ac-t]
    [clojucture.asset-test :as at-t]
    )
  (:import [tech.tablesaw.api Row]
           [java.time LocalDate])
  )

(def test-pool
  (p/->pool [ at-t/test-mortgage ])
  )

(deftest pool-agg-test
  (let [ pool-agg (.collect-cashflow test-pool "D" [(jt/local-date 2018 3 1) (jt/local-date 2018 9 1)] )
    ]
    (println pool-agg)
    )
  )


(deftest deposit-from-pool
  (let [ pool-cf  cf-t/sample-cf
        pool-cf-collect (.aggregateByInterval pool-cf "" (into-array LocalDate [ (jt/local-date 2018 2 1) (jt/local-date 2018 6 1) ]))
        accs   {:acc1 ac-t/t-account-1 :acc2 ac-t/t-account-2 }
        mp {:principal :acc1 :interest :acc2}
        deposit-date (jt/local-date 2018 2 2)
        _ (println pool-cf-collect)
        accs-result (p/deposit-to-accs pool-cf-collect accs mp deposit-date)
        accs-int (:acc2 accs-result)
        accs-prin (:acc1 accs-result)
        ]
    ;(println (keys accs-result))
    ;(is (= 3400.0 (:balance accs-prin)))
    ;(is (= 12 (count (:stmts accs-prin))))

    ;(is (= 660.0 (:balance accs-int)))
    ;(is (= 12 (count (:stmts accs-int))))
    )
  )
