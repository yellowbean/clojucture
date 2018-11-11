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


