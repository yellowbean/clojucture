(ns clojucture.helper
  (:require [clojure.test :refer :all]

            ))

(defn close?
  ([x y ]
   (close? x y 0.01))

  ([ x y eps]
   (< (Math/abs (- x y)) eps)))
