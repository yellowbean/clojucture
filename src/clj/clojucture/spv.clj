(ns clojucture.deal
  (:require [clojucture.bond :as b]
            [clojucture.asset :as a]
            [clojucture.type :as t]
            [java-time :as jt]
            [clojucture.util :as u]
            [clojure.java.io :as io]
            [clojure.core.match :as m])
  (:import [tech.tablesaw.api ColumnType Table]
           [tech.tablesaw.columns AbstractColumn Column]
           [java.time LocalDate]
           )

  )

(defn gen-pool-collect-interval [ deal-info ]
  (let [{ closing-date :closing-date
          first-collect-date :first-collect-date
          collect-interval :collect-interval
          stated-maturity :stated-maturity } (:dates deal-info) ]
    (->
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
  (let [{
         first-payment-date :first-payment-date
         payment-interval   :payment-interval
         stated-maturity    :stated-maturity} (:dates deal-info)]
      (case payment-interval
        :Q (u/gen-dates-range first-payment-date (jt/months 3) stated-maturity)
        :M (u/gen-dates-range first-payment-date (jt/months 1) stated-maturity)
        :BM (u/gen-dates-range first-payment-date (jt/months 2) stated-maturity)
        :Y (u/gen-dates-range first-payment-date (jt/years 1) stated-maturity))
      ))

(defn init-bondflow [ deal-info bonds ]
  (let [ payment-dates (gen-bond-payment-date deal-info)
        default-fields [ [:double :balance] [:double :principal] [:double :interest]
                        [:double :loss] [:double :int] [:double :arrears]]
        tb (u/init-table "Bond Cashflow" default-fields)
        dates-column (into-array AbstractColumn [(u/gen-column [:dates payment-dates])])
        ]
    (.addColumns dates-column tb) ) )

(defrecord deal [ info status pool waterfall bond ])


(defn init-deal [ d ]
  (when-let [deal d]
    (let [ i (:info deal)
          update-date (get-in d [:status :update-date])]
      (println i)
      (-> d
          ;; deal original info
          (assoc-in [:info :p-collection-intervals] (gen-pool-collect-interval i))
          (assoc-in [:info :b-payment-dates] (gen-bond-payment-date i))


          ;; update info
          (assoc-in [:status :b-rest-payment-dates]
                    (filter #(jt/after? % update-date) (get-in d [:info :b-payment-dates])))
          (assoc-in [:status :p-rest-collection-intervals]
                    (filter #(jt/after? % update-date) (get-in d [:info :p-collection-intervals]))
                    )

          )
      )
    )
  )


(defn build-deal [ m ]
  (->
    (m/match m
    {:info i :status s :pool p :waterfall w :bond b}
      (deal. i s p w b )
    :else nil
    )
    (init-deal))
  )




(defn run-assets [ d assump ]
  (let [ pool-collect-int (gen-pool-collect-interval (:info d))
         pool-cf (.collect-cashflow (:pool d) assump pool-collect-int)
        ]
    (-> d
      (assoc-in [:status :asset-run?] true)
      (assoc-in [:status :pool-cashflow] pool-cf))
  )
)

(defn run-bonds [ d ]
  (let [ {update-date :update-date} (:status d)
        bond-rest-payment-dates (drop-while #(jt/before? % update-date) (get-in (:info d) :b-payment-dates))

        ]


    )

  )
