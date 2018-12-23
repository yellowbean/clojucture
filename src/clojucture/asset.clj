(set! *warn-on-reflection* true)
(ns clojucture.asset
  (:require [java-time :as jt]
            [clojucture.util :as u]
            [clojucture.type :as t]
            [clojucture.assumption :as a]

            )
  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn StringColumn BooleanColumn]
    [tech.tablesaw.columns AbstractColumn]
    [java.time LocalDate]
    [java.util Arrays]
    )
  )

(defn -loan-gen-prin [term balance opt]
  "Generate principal vector for loan with option of schedule principal"
  (let [principal-flow (double-array term 0)]
    (if (get opt :schedule-principal false)
      (doseq [[i pmt] (opt :schedule-principal)]
        (aset-double principal-flow i pmt)))
    (aset-double principal-flow (dec term) balance)
    principal-flow))


(defrecord mortgage [ start_date periodicity term period_rate balance opt]
  t/Asset
  (project-cashflow [ x ]
    (let [ term (inc term)
      dates (u/gen-dates-ary start_date periodicity term)
      bal (double-array term 0)
      prin (double-array term 0)
      int (double-array term 0)
      period_pmt (u/period-pmt balance term period_rate)
      even_principal_pmt (/ balance term)]
      (aset-double bal 0 balance)
      (doseq [ i (range  1 term)]
        (let [ last_period_balance (aget bal (dec i ))
               period_interest (* last_period_balance period_rate)
               period_principal (if (false? (get opt :even-principal false))
                                      (- period_pmt period_interest)
                                      even_principal_pmt)]
          (aset-double bal i (- last_period_balance period_principal))
          (aset-double prin i period_principal)
          (aset-double int i period_interest)
          ))
      (u/gen-table "cashflow"
         {:dates dates :balance bal :principal prin :interest int}))
  )
  (project-cashflow [ x assump ]
    nil
    )
)



(defrecord float-mortgage [ ^LocalDate start-date periodicity ^Integer term ^Integer remain-term ^Double current-rate ^Double current-balance day-count float-info opt]
  t/Asset
  (project-cashflow [x]
    nil
    )

  (project-cashflow [x assump]
    (let [term                      (inc term)
          dates                     (u/gen-dates-ary start-date periodicity term)
          bal                       (double-array term 0)
          _                         (aset-double bal 0 current-balance)
          prin                      (double-array term 0)
          int                       (double-array term 0)
          even_principal_pmt        (/ current-balance term)
          reset-dates               (:reset-dates float-info)
          future-rates-of-asset     (a/apply-curve (:indexes assump) float-info reset-dates)
          period-annual-rate-vector (u/gen-float-period-rates dates future-rates-of-asset)
          period-rate-vector        (map #(u/get-period-rate periodicity % day-count) period-annual-rate-vector)
          ]
      (doseq [i (range 1 term)]
        (let [last_period_balance (aget bal (dec i))
              period_interest     (* last_period_balance (nth period-rate-vector i))
              period_pmt          (u/period-pmt current-balance (inc term) (nth period-rate-vector i))
              period_principal    (if (false? (get opt :even-principal false))
                                    (- period_pmt period_interest)
                                    even_principal_pmt)]
          (aset-double bal i (- last_period_balance period_principal))
          (aset-double prin i period_principal)
          (aset-double int i period_interest)
          ))

      (u/gen-table "cashflow"
                   {:dates    dates :balance bal :principal prin
                    :interest int :rate (double-array period-rate-vector)})
      )
    ))








(defrecord loan [ start_date periodicity term rate balance day_count opt]
  t/Asset
  (project-cashflow [ x ]
    (let [
          term (inc term)
          dates (u/gen-dates-ary start_date periodicity term)
          prin (-loan-gen-prin term balance opt)
          bal (u/gen-balance prin balance)
          accrued-int (u/gen-accrued-interest bal dates rate day_count)

          int (u/gen-interest accrued-int opt)
          ]

      (u/gen-table "cashflow"
         {:dates dates :balance bal :principal prin :accrued-interest accrued-int :interest int} )
    ))
  (project-cashflow [ x assump]
    nil
    )
)



(defrecord float-loan [ start_date periodicity term rate balance day-count float-info opt ]
  t/Asset
  (project-cashflow [ x ]
    nil
    )
  (project-cashflow [ x assump ]
    (let [term (inc term)
          dates (u/gen-dates-ary start_date periodicity term)
          prin (-loan-gen-prin term balance opt)
          bal (u/gen-balance prin balance)

          reset-dates (take-nth (:reset-period float-info) dates)
          future-rates (a/apply-curve (:indexes assump) float-info reset-dates)
          period-rate-vector (u/gen-float-period-rates dates future-rates )
          accrued-int (u/gen-vector-accrued-interest bal dates period-rate-vector day-count periodicity)
          int (u/gen-interest accrued-int opt)
          ]
      (u/gen-table "cashflow"
                  {:dates dates :balance bal :principal prin
                    :accrued-interest accrued-int :interest int} )
      )
    )
  )

(defrecord commercial-paper [ balance start-date  end-date opt ]
  t/Asset
  (project-cashflow [ x ]
    (let [
           dates (into-array LocalDate [start-date end-date])
           bal (double-array [balance 0])
           prin (double-array [0 balance ])
          ]
      (u/gen-table "cashflow"
                   {:dates dates :balance bal :principal prin } )
      )
    )
  )


(defrecord installments [ balance start-date periodicity term period-fee-rate opt ]
  t/Asset
  (project-cashflow [ x ]
    (let [
           dates (u/gen-dates-ary start-date periodicity (inc term))
           even-principal (/ balance term)
           even-principal-list (->> (take term (repeat even-principal)) (cons 0))
           prin  (double-array even-principal-list)
           bal (u/gen-balance prin balance)
           period-fee (* balance period-fee-rate)
           period-fee-list (->> (take term (repeat period-fee)) (cons 0))
           fee  (double-array period-fee-list)
          ]
      (u/gen-table "cashflow"
                   {:dates dates :balance bal :principal prin :installment-fee fee} )
      )
    )
  (project-cashflow [ x assump ]
    nil )
  )



(defn -leasing-gen-deposit-flow [ term opt ]
  (let [ {deposits-amount :deposit-balance :or {deposits-amount 0}} opt
         deposit-empty-flow (double-array term)
        ]
    (do
      (aset-double deposit-empty-flow 0  deposits-amount)
      (aset-double deposit-empty-flow (dec term) (- deposits-amount))
      deposit-empty-flow)))


(defrecord leasing [ start-date term periodicity rental opt ]
  t/Asset
  (project-cashflow [ x ]
    (let [ dates (u/gen-dates-ary start-date periodicity (inc term))
           rental-flow-list (->> (take term (repeat rental)) (cons 0))
           rental-flow (double-array rental-flow-list) ]
      (if (get opt :deposit-balance false)
        (u/gen-table "cashflow"
                     {:dates dates :rental rental-flow :deposit (-leasing-gen-deposit-flow (inc term) opt)} )
        (u/gen-table "cashflow"
                     {:dates dates :rental rental-flow} )
        )))

  (project-cashflow [ x assump ])
  )
