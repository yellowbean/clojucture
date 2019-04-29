(ns clojucture.spv
  (:require [clojucture.bond :as b]
            [clojucture.asset :as a]
            [clojucture.type :as t]
            [java-time :as jt]
            [clojucture.util :as u]
            [clojure.java.io :as io]
            [clojure.core.match :as m]
            [clojure.pprint :as pp])
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
      (conj closing-date)
      (u/gen-dates-interval )
      )
  )
)

(defn gen-bond-payment-date [ deal-info ]
  (let [{
         first-payment-date :first-payment-date
         payment-interval   :payment-interval
         stated-maturity    :stated-maturity} (:dates deal-info)]
    (->
      (case payment-interval
        :Q (u/gen-dates-range first-payment-date (jt/months 3) stated-maturity)
        :M (u/gen-dates-range first-payment-date (jt/months 1) stated-maturity)
        :BM (u/gen-dates-range first-payment-date (jt/months 2) stated-maturity)
        :Y (u/gen-dates-range first-payment-date (jt/years 1) stated-maturity))
      (vec)
      (conj stated-maturity)(seq))
    )
  )

(defn gen-pool-cf-split-dates [ deal-info ]
  (let [ closing-date (get-in deal-info [:dates :closing-date])
        dd (get-in deal-info [:dates :delay-days])
        split-dates (map #(jt/adjust % jt/minus (jt/days dd))  (deal-info :b-payment-dates)) ]
    (->>
      (cons closing-date split-dates)
      (partition 2 1)) ) )

(defn init-bondflow [ deal-info bonds ]
  (let [ payment-dates (gen-bond-payment-date deal-info)
        default-fields [ [:double :balance] [:double :principal] [:double :interest]
                        [:double :loss] [:double :int] [:double :arrears]]
        tb (u/init-table "Bond Cashflow" default-fields)
        dates-column (into-array AbstractColumn [(u/gen-column [:dates payment-dates])])
        ]
    (.addColumns dates-column tb) ) )

;; simple deal structure without any localization
;; (defrecord deal [ info status pool waterfall bond ])
(defn b-find-cutoff [ d ^LocalDate payment-date ]
  (let [ dd (get-in d [:info :delay-days])]
    
    
    )
  
  )

(defn init-deal [ d ]
  (when-let [deal d]
    (let [ update-date (get-in deal [:status :update-date])]
;      (println (gen-pool-cf-split-dates i))
      (as-> deal deal-setup
          ;; deal original info
          (assoc-in deal-setup [:info :p-collection-intervals] (gen-pool-collect-interval (deal-setup :info)))
          (assoc-in deal-setup [:info :b-payment-dates] (gen-bond-payment-date (deal-setup :info)))
          (assoc-in deal-setup [:info :b-aggregate-intervals] (gen-pool-cf-split-dates (deal-setup :info)))


          ;; update info
          (assoc-in deal-setup [:status :b-rest-payment-dates]
                    (filter #(jt/after? % update-date) (get-in deal-setup [:info :b-payment-dates])))
          (assoc-in deal-setup [:status :p-rest-collection-intervals]
                    (filter #(jt/after? (first %) update-date) (get-in deal-setup [:info :p-collection-intervals])) )

          ;; update expense
          ;; (assoc-in deal-setup [:status :expense] )
          ;; update pool

          ;; update bond
          ;;

        )
      )
    )
  )


(defn build-deal [ m ]
  (->
    (m/match m
    {:info i :status s }
      m
    :else nil
    )
    (init-deal))
  )


(defn run-assets [ d assump ]
  (let [ pool-collect-int (gen-pool-collect-interval (:info d)) ]
    (-> {:deal d}
      (assoc :pool-cf (.collect-cashflow (get-in d [:status :pool]) assump pool-collect-int) )
      (assoc :assumption assump))
  )
)

(defn run-bonds [ d assump ]
  (let [ bond-rest-payment-dates (get-in d [:info :b-rest-payment-dates ]) ]
    (doseq [ pd bond-rest-payment-dates]
      
      
      )
    nil
    )
)
