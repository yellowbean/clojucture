
(comment
(ns clojucture.CN-reader-test
  (:require [clojure.test :refer :all]
            ;[clojucture.deal :as d]
            [clojucture.util :as u]
            [clojucture.CN-reader :as r]
            [clojure.xml :as xml ]
            [clojure.java.io :as io]
            [clojure.data.zip.xml :as zip-xml]
            [clojure.zip :as z])

  (:use midje.sweet)
  )

(def test-cn-deal-xml (r/read-chinese-deal (io/resource "china/china-bank-deal-1.xml")))

(fact "load a chinese interbank deal"
      (count (:accounts test-cn-deal-xml)) => 5
      (count (:bonds test-cn-deal-xml)) => 3
      ;(count (:pool test-cn-deal-xml)) => >=1
  )

)