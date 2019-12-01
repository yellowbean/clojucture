(ns clojucture.testing-utils
  (:require [clojure.test :refer :all]))



(defn close-to
  ([x y]
   (close-to x y 0.001)
   )
  ([x y z ]
   (if (< (Math/abs (- x y)) z)
     true
     false)
   )
  )
