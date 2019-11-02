(ns clojucture.account-test
  (:require
    [java-time :as jt]
    [clojucture.account :as acc]
    [clojure.test :refer :all])
  )

(def t-account-1 (acc/->account :acc1 :prin 1000 []))
(def t-account-2 (acc/->account :acc2 :int 0 []))

(deftest deposit-test
  (let [test-account (acc/->account :acc-1 :cash 0 []) ]
    (is (:balance (.deposit test-account (jt/local-date 2018 11 1) :originator 200)) 200)
    )
  )

(deftest withdraw-test
  (let [test-account (acc/->account :acc-1 :cash 1000 [])
        ]
    (is (:balance (.withdraw test-account (jt/local-date 2018 11 1) :investor 600)) 400)
    )
  )

(deftest try-withdraw-test
  (let [test-account-1 (acc/->account :from :cash 1000 [])
        test-account-1-after-try (.try-withdraw test-account-1 (jt/local-date 2018 11 12) :to 1500)
        ]
    (is (= (:balance test-account-1-after-try) 0))
    )
  )


(deftest transfer-test
  (let [test-account-1 (acc/->account :from :cash 1000 [])
        test-account-2 (acc/->account :to :cash 1000 [])
        [acc-1 acc-2] (acc/transfer-fund test-account-1 test-account-2 (jt/local-date 2018 11 11) 300)
        [acc-1-2 acc-2-2] (acc/transfer-fund acc-1 acc-2 (jt/local-date 2018 11 12) 20)
        ]

    ; statement test after first transfer
    (is (= (:balance acc-1) 700))
    (is (= (:balance acc-2) 1300))


    (is (= 1 (count (:stmts acc-1))))
    (is (= 1 (count (:stmts acc-2))))

    (is (= -300 (:amount (first (:stmts acc-1)))))
    (is (= 300 (:amount (first (:stmts acc-2)))))


    ; statement test after 2nd transfer
    (is (= (:balance acc-1-2) 680))
    (is (= (:balance acc-2-2) 1320))


    (is (= 2 (count (:stmts acc-1-2))))
    (is (= 2 (count (:stmts acc-2-2))))

    (is (= -300 (:amount (first (:stmts acc-1-2)))))
    (is (= -20 (:amount (second (:stmts acc-1-2)))))

    (is (= 300 (:amount (first (:stmts acc-2-2)))))
    (is (= 20 (:amount (second (:stmts acc-2-2)))))

    ))


(deftest transfer-funds-test
  (let [test-account-1 (acc/->account :from :cash 1000 [])
        test-account-2 (acc/->account :to :cash 1000 [])
        test-account-3 (acc/->account :to :cash 0 [])
        [acc-1 acc-2] (acc/transfer-fund test-account-1 test-account-2 (jt/local-date 2018 1 1))

        [acc-result-map acc-target] (acc/transfer-funds {:a test-account-1 :b test-account-2}
                                                    test-account-3 (jt/local-date 2018 1 1))
        ]
    (is (= (:balance acc-1) 0))
    (is (= (:balance acc-2) 2000))


    (is (= (:balance acc-target) 2000))
    (is (= [ 0 0 ]   (mapv :balance (vals acc-result-map) )))
    ))


(deftest tReserveAccount
  (let [test-account-1 (acc/->account :from :cash 1000 [])
        rAcc1 (acc/->reserve-account "rAcc1" {:target 1000} 500 [])
        ;rAcc2 (acc/->reserve-account "rAcc2" {:target 1000} 1000 [])

        [acc-1 acc-2] (acc/transfer-fund test-account-1 rAcc1 (jt/local-date 2019 1 1) 400)
        [acc-12 acc-22] (acc/transfer-fund test-account-1 rAcc1 (jt/local-date 2019 1 1) 600)
        ]

    ;transfer not meet target balance
    (is (= (:balance acc-1) 600))
    (is (= (:balance acc-2) 900))

    ;transfer meet target balance
    (is (= (:balance acc-12) 500))
    (is (= (:balance acc-22) 1000))                         ;reserve account shall only accept fund up to balance = 1000

    )
  )


(deftest tUpdateTargetAcc
  (let [test-acc (acc/->account :t nil 1000 [])
        test-acc2 (acc/->account :t nil 1000 [])
        test-acc3 (acc/->account :t nil 1000 [])
        [new-from new-to] (acc/transfer-fund test-acc test-acc2 (jt/local-date 2019 1 1) 500)
        bench-acc (acc/update-target-account new-from test-acc3)
        ]
    (is (= (:balance new-to) (:balance bench-acc)))
    (is (= (:amount (.last-txn new-to)) (:amount (.last-txn bench-acc))))

    ))