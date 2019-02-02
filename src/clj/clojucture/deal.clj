(ns clojucture.deal
  (:require [clojucture.bond :as b]
            [clojucture.asset :as a]
            [clojucture.type :as t]
            [java-time :as jt]
            [clojucture.util :as u]
            [clojure.java.io :as io])
  (:import [tech.tablesaw.api ColumnType Table]
           [tech.tablesaw.columns AbstractColumn Column]
           [java.time LocalDate]
           )

  )

(defprotocol Deal
  (run-assets [ x ] [ x assump ] )
  (run-bonds  [ x ] [ x assump ] )
  (run-deal [ x ] [ x assump ])
  (run-triggers [ x ])
  )

(defn gen-pool-collect-interval [ deal-info ]
  (let [{ closing-date :closing-date
          first-collect-date :first-collect-date
          collect-interval :collect-interval
          stated-maturity :stated-maturity } deal-info ]
    (->>
      (case collect-interval
        :Q (u/gen-dates-range first-collect-date (jt/months 3) stated-maturity)
        :M (u/gen-dates-range first-collect-date (jt/months 1) stated-maturity)
        :BM (u/gen-dates-range first-collect-date (jt/months 2) stated-maturity)
        :Y (u/gen-dates-range first-collect-date (jt/years 1) stated-maturity))
      (cons closing-date)
      (u/gen-dates-interval )
      )
  )
)

(defn gen-bond-payment-date [ deal-info ]
  (let [{settle-date       :settle-date
         first-payment-date :first-payment-date
         payment-interval   :payment-interval
         stated-maturity    :stated-maturity} deal-info]
    (->>
      (case payment-interval
        :Q (u/gen-dates-range first-payment-date (jt/months 3) stated-maturity)
        :M (u/gen-dates-range first-payment-date (jt/months 1) stated-maturity)
        :BM (u/gen-dates-range first-payment-date (jt/months 2) stated-maturity)
        :Y (u/gen-dates-range first-payment-date (jt/years 1) stated-maturity))
      (cons settle-date)
      )
    )
  )




;(.collect pool)