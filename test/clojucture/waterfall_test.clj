(ns clojucture.waterfall_test
  (:require [clojure.test :refer :all]
            [clojure.core.match :as m]
            [clojure.zip :as zip]

            ))




(def simple-waterfall
  [:default-event?
   [[:A-1
     [:accelerate?
      [[:acc-1
        :acc-2
        :acc-3]
       [
        :not-acc-action-1
        :not-acc-action-2]]
      ]
     ]
    [:B]]
   ])

(def simple-wf
  [:default-event?
   [
    [:T-1 :T-2
     [:acc?
      [
       [:T-ACC1 :T-ACC2]
       [:F-ACC1]
       ]
      ]
     ]
    [:F-1 :F-2]
    ]
   ]
  )

(def simple-wf-t (zip/vector-zip simple-wf))





