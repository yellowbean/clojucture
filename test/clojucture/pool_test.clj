(ns clojucture.pool-test
  (:require
    [java-time :as jt]
    [clojure.test :refer :all]
    [clojucture.asset :as asset]
    [clojucture.assumption :as a]
    [clojucture.account :as acc]
    [clojucture.pool :as p]
    [clojucture.util :as u]
    [clojucture.cashflow-test :as cf-t]
    [clojucture.account-test :as ac-t]
    [clojucture.testing-utils :as tu]
    [clojucture.asset-test :as at-t])

  (:import [tech.tablesaw.api Row]
           [java.time LocalDate]
           [clojucture RateAssumption]))


(def test-pool (atom nil))

(def t-account-1 (acc/->account :acc1 :prin 1000 []))
(def t-account-2 (acc/->account :acc2 :int 0 []))
(def test-mortgage
  (asset/->mortgage
    {:start-date (jt/local-date 2014 5 5) :periodicity (jt/months 1) :term 48 :balance 20000 :period-rate 0.01}
    {:last-paid-date nil}
    20000 0.01 48 nil))

(defn init-t-pool [tst]
  (reset! test-pool (p/->pool [at-t/test-mortgage] (jt/local-date 2018 1 1)))
  (tst)
  )


(use-fixtures :each init-t-pool)


(deftest pool-cf-test
  (let [pool-cf (.project-cashflow (p/->pool [at-t/test-mortgage] (jt/local-date 2016 6 1)))
        sum-int (-> (.column pool-cf "interest") (.sum))
        sum-prin (-> (.column pool-cf "principal") (.sum))
        ]
    (is (< (Math/abs (- sum-int 1451.84373)) 0.001))
    (is (< (Math/abs (- sum-prin 11188.39728)) 0.001))

    (is (= (.rowCount pool-cf) 24))
    )
  )

(deftest pool-agg-test
  (let [pool-cf (->
                  (p/->pool [at-t/test-mortgage] (jt/local-date 2016 6 1))
                  (.collect-cashflow nil [(jt/local-date 2017 1 1) (jt/local-date 2018 1 1)]))

        prin-col (.column pool-cf "principal")
        [p1 p2 p3] (map #(.get prin-col %) (range 3))
        int-col (.column pool-cf "interest")
        [i1 i2 i3] (map #(.get int-col %) (range 3))
        beg-date-col (.column pool-cf "starting-date")
        [b1 b2 b3] (map #(.get beg-date-col %) (range 3))
        end-date-col (.column pool-cf "ending-date")
        [e1 e2 e3] (map #(.get end-date-col %) (range 3))
        rc (.rowCount pool-cf)
        ]
    (is (= 3 rc))

    (is (tu/close-to p1 2992.122))
    (is (tu/close-to p2 5640.0861))
    (is (tu/close-to p3 2556.1892))
    (is (tu/close-to i1 694.6150))
    (is (tu/close-to i2 680.0344))
    (is (tu/close-to i3 77.1944))
    (is (= b1 (jt/local-date 2016 6 5)))
    (is (= b2 (jt/local-date 2017 1 1)))
    (is (= b3 (jt/local-date 2018 1 1)))
    (is (= e1 (jt/local-date 2016 12 31)))
    (is (= e2 (jt/local-date 2017 12 31)))
    (is (= e3 (jt/local-date 2018 5 5)))


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
    (let [pool-cf (.project-cashflow @test-pool nil)
          pool-cf-cum (p/calc-cumulative-amount pool-cf "interest")
          cum-column (.column pool-cf-cum "interest[cumSum]")
          ]
      (is (= (.getDouble cum-column 0) 0.0))
      (is (= (.getDouble cum-column 1) 200.0))
      (is (< (- (.getDouble cum-column 11) 2014.82831679) 0.001))
      ))
  )
