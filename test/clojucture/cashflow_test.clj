(ns clojucture.cashflow-test
  (:require [clojure.test :refer :all]
            [clojucture.asset :as asset]
            [java-time :as jt]
            )
)


(deftest cf-test []
 (let [ m {:balance 500 :term 10 :periodicity (jt/months 1)}]

   )
)