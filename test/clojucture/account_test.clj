(ns clojucture.account-test
  (:require
    [java-time :as jt]
    [clojucture.account :as acc ]
    [clojure.test :refer :all])
  (:use midje.sweet)
  )


; account test
(deftest deposit-test
         (let [ test-account (acc/->account :acc-1 :cash 0 {})
               ]
           (is (:balance (.deposit test-account (jt/local-date 2018 11 1) :originator 200)) 200)
           )
         )


(deftest withdraw-test
  (let [ test-account (acc/->account :acc-1 :cash 1000 {})
        ]
    (is (:balance (.withdraw test-account (jt/local-date 2018 11 1) :investor 600)) 400)
    )
  )

(deftest transfer-test
  (let [ test-account-1 (acc/->account :from :cash 1000 {})
        test-account-2 (acc/->account :to :cash 1000 {})
        [ acc-1 acc-2 ] (acc/transfer-fund test-account-1 test-account-2 (jt/local-date 2018 11 11) 300)
        ]
    (is (= (:balance acc-1)  700))
    (is (= (:balance acc-2)  1300))
    )
  )