(ns clojucture.waterfall
  (:require [clojure.test :refer :all]))




(def simple-waterfall
  [
    (list :pay-expense :cash :tax )
    (list :pay-expense :cash :servicing-fee)
    (list :pay-expense :cash :manage-fee)
    (list :pay-interest :cash :A)
    (list :pay-principal :cash :A)
    (list :transfer :cash :reserve)
   ]
  )
