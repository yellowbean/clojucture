(ns clojucture.pool-test
  (:require
    [java-time :as jt]
    [clojure.test :refer :all]
    [clojucture.asset :as asset ]
    [clojucture.pool :as p]
    [clojucture.util :as u]
    [clojucture.cashflow-test :as cf-t]
    [clojucture.account-test :as ac-t]
    )
  (:import [tech.tablesaw.api Row])
  )
(comment
  (def date-intervals
    (u/gen-dates-interval
      [(jt/local-date 2016 4 1) (jt/local-date 2018 4 1) (jt/local-date 2023 1 2)]))


  (deftest test-pool-cf
    (let [lm1 {:info {:start-date (jt/local-date 2018 3 1)} }

          l-list (asset/map->loan lm1)
          tp (p/->pool [])

          ])

    )
  )


(deftest deposit-from-pool
  (let [ pool-cf  cf-t/sample-cf
        accs   {:acc1 ac-t/t-account-1 :acc2 ac-t/t-account-2 }
        mp {:principal :acc1 :interest :acc2}
        deposit-date (jt/local-date 2018 2 2)
        accs-result (p/deposit-to-accs pool-cf accs mp deposit-date)
        accs-int (:acc2 accs-result)
        accs-prin (:acc1 accs-result)
        ]
    ;(println (keys accs-result))
    (is (= 3400.0 (:balance accs-prin)))
    (is (= 12 (count (:stmts accs-prin))))

    (is (= 660.0 (:balance accs-int)))
    (is (= 12 (count (:stmts accs-int))))
    )
  )
