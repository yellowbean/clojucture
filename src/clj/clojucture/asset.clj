(ns clojucture.asset
  (:require [java-time :as jt]
            [clojucture.util :as u]
            [clojucture.type :as t]
            [clojucture.assumption :as a]
            [clojure.core.match :as m])
  (:import
    [java.time LocalDate]
    [org.apache.commons.lang3 ArrayUtils]))



(defn -loan-gen-prin [term balance opt]
  "Generate principal vector for loan with option of schedule principal"
  (let [principal-flow (double-array term 0)]
    (if (get opt :schedule-principal false)
      (doseq [[i pmt] (opt :schedule-principal)]
        (aset-double principal-flow i pmt)))
    (aset-double principal-flow (dec term) balance)
    principal-flow))


(defn get-current-pmt [ history info]
  (if-let [reset-info (:last-pmt-reset history)]
    (u/period-pmt (:balance reset-info) (:term reset-info) (:period-rate reset-info))
    (u/period-pmt (:balance info ) (:term info) (:period-rate info))))



(defn -gen-interest-rate [ info d assump]
  (m/match info
    {:float-info fi}
    (a/apply-curve (:index-curves assump) fi)
    :else
      nil))



(defn -gen-assump-curve [ ds assump]
  (let [ ppy-curve (:prepayment assump)
        def-curve (:default assump)
        [ recover-curve recovery-lag ] (:recovery assump)]


    nil))



(defrecord mortgage [ info history current-balance period-rate remain-term opt]
  t/Asset
  (project-cashflow [ x ]
    (let [ nil-assumption {:prepayment nil :default nil :recovery [ nil nil ]}]
      (.project-cashflow x nil-assumption) ) )

  (project-cashflow [ x assump ]
    (let
      [ { start_date :start-date  periodicity :periodicity  term :term balance :balance}   info
       cf-length (inc remain-term)
       dates (u/gen-dates-ary start_date periodicity (inc term))
       remain-dates (ArrayUtils/subarray dates  (- term remain-term ) (inc term))

      ;init empty cashflow
       period_pmt (get-current-pmt history info)
       even_principal_pmt (/ balance term)

      ; project interest rates
       projected-interest-rates (-gen-interest-rate info nil assump) ; interest rates
      ; prepayment rates
       projected-prepayment-rate (-gen-assump-curve remain-dates assump)
      ; default rates
       projected-default-rate (-gen-assump-curve remain-dates assump)]
      ; recovery rates
      (loop [  payment-dates remain-dates paid-dates [ (jt/local-date 1900 1 1)] bal-list [current-balance] prin-list [ 0 ] int-list [ 0 ] ]
        (if-let [ current-pay-date (second payment-dates) ]
          (let [ last-pay-date (first payment-dates)
                  f-bal (last bal-list)
                  int-amount (* f-bal period-rate)
                  prin-amount (- period_pmt int-amount)
                  n-bal (- f-bal prin-amount)
                ]


            (recur (next payment-dates)
                   (conj paid-dates current-pay-date)
                   (conj bal-list n-bal)
                   (conj prin-list prin-amount)
                   (conj int-list int-amount))
            )
          (u/gen-table "cashflow"
             {:dates (u/dates paid-dates) :balance (double-array bal-list)
              :principal (double-array prin-list) :interest (double-array int-list)}   ) )
        )
      )))





(defrecord float-mortgage [ ^LocalDate start-date periodicity ^Integer term ^Integer remain-term ^Double current-rate ^Double current-balance day-count float-info opt]
  t/Asset
  (project-cashflow [ x ]
    nil)


  (project-cashflow [x assump ]
    (let [term                      (inc term)
          dates                     (u/gen-dates-ary start-date periodicity term)
          bal                       (double-array term 0)
          _                         (aset-double bal 0 current-balance)
          prin                      (double-array term 0)
          int                       (double-array term 0)
          even_principal_pmt        (/ current-balance term)
          reset-dates               (:reset-dates float-info)
          future-rates-of-asset     (a/apply-curve (:indexes assump) float-info)
          period-annual-rate-vector (u/gen-float-period-rates dates future-rates-of-asset)
          period-rate-vector        (map #(u/get-period-rate periodicity % day-count) period-annual-rate-vector)]

      (doseq [i (range 1 term)]
        (let [last_period_balance (aget bal (dec i))
              period_interest     (* last_period_balance (nth period-rate-vector i))
              period_pmt          (u/period-pmt current-balance (inc term) (nth period-rate-vector i))
              period_principal    (if (false? (get opt :even-principal false))
                                    (- period_pmt period_interest)
                                    even_principal_pmt)]
          (aset-double bal i (- last_period_balance period_principal))
          (aset-double prin i period_principal)
          (aset-double int i period_interest)))


      (u/gen-table "cashflow"
                   {:dates    dates :balance bal :principal prin
                    :interest int :rate (double-array period-rate-vector)}))))










(defrecord loan [ info current-balance remain-term current-rate opt]
  t/Asset
  (project-cashflow [ x ]
    (let [  {first-pay :first-pay maturity-date :maturity-date }  info
	        def-a (a/gen-pool-assump-df :cdr [0 0] [first-pay maturity-date])
	        ppy-a (a/gen-pool-assump-df :cpr [0 0] [first-pay maturity-date])
			empty-assumption {:prepayment  ppy-a :default def-a}
		]
		(.project-cashflow x empty-assumption)))
    
  (project-cashflow [ x assump ]
    (let [ { start-date :start-date first-pay :first-pay periodicity :periodicity
            maturity-date :maturity-date term :term rate :rate balance :balance} info
           payment-dates (->
                           (cons start-date
                                 (take term (u/gen-dates-range first-pay periodicity)))
                           (vec) (conj maturity-date))
           rest-payment-dates (subvec (vec payment-dates) (- term  remain-term))
           _ (println "Rest payment dates" (first rest-payment-dates))
           next-payment-date (first rest-payment-dates)
           last-payment-date (nth (vec payment-dates)  (- term remain-term 2))
           { ppy-assump :prepayment def-assump :default } assump
          ppy-assump-proj (a/gen-asset-assump ppy-assump rest-payment-dates)
          def-assump-proj (a/gen-asset-assump def-assump rest-payment-dates)
          ;result column
          projection-periods  (count rest-payment-dates)
          bal (double-array projection-periods)
          prin (double-array projection-periods)
          interest (double-array projection-periods)
          prepay (double-array projection-periods)
          default (double-array projection-periods)
          _ (aset-double bal 0 current-balance)
          payment-dates-int (u/gen-dates-interval rest-payment-dates)
          ]
      ;(println "first proj date" (first rest-payment-dates))
      (loop [ pds payment-dates-int ppy-a (seq ppy-assump-proj) def-a (seq def-assump-proj)
             f-bal current-balance p-dates [last-payment-date]  idx 1]
        (if (or (nil? pds) (< f-bal 0.01))
            (u/gen-cashflow "CF"
                            {:dates      (u/dates p-dates) :balance bal :principal prin :interest interest
                             :prepayment prepay :default default})
          (let [ df-amt (* f-bal (first def-a)) ; default amount
                py-amt (* f-bal (first ppy-a)) ; prepayment amount
                next-balance (- f-bal df-amt py-amt)
                pd (first pds) ;; current payment date interval
                ;_ (prn "F I" (first pd) "E I" (second pd))
                int-amount (* f-bal (u/cal-period-rate (first pd) (second pd) current-rate :30_365))
                ;_ (println  (first pd) (second pd)   "INT AMT" int-amount)
                ]
           (aset-double bal idx next-balance)
           (aset-double interest idx int-amount)
           (aset-double prepay idx py-amt)
           (aset-double default idx df-amt)


           (if (= idx (dec projection-periods))
               (aset-double prin idx next-balance))
           (recur (next pds) (next ppy-a) (next def-a) next-balance (conj p-dates (first pd)) (inc idx))

             )))))
)






    (defrecord commercial-paper [balance start-date end-date opt]
      t/Asset
      (project-cashflow [x]
        (let [
              dates (into-array LocalDate [start-date end-date])
              bal (double-array [balance 0])
              prin (double-array [0 balance])]

          (u/gen-table "cashflow"
                       {:dates dates :balance bal :principal prin})))
      (project-cashflow [x assump]

        nil))



    (defrecord installments [balance start-date periodicity term period-fee-rate opt]
      t/Asset
      (project-cashflow [x]
        (let [
              dates (u/gen-dates-ary start-date periodicity (inc term))
              even-principal (/ balance term)
              even-principal-list (->> (take term (repeat even-principal)) (cons 0))
              prin (double-array even-principal-list)
              bal (u/gen-balance prin balance)
              period-fee (* balance period-fee-rate)
              period-fee-list (->> (take term (repeat period-fee)) (cons 0))
              fee (double-array period-fee-list)]

          (u/gen-table "cashflow"
                       {:dates dates :balance bal :principal prin :installment-fee fee})))


      (project-cashflow [x assump]
        nil))




    (defn -leasing-gen-deposit-flow [term opt]
      (let [{deposits-amount :deposit-balance :or {deposits-amount 0}} opt
            deposit-empty-flow (double-array term)]

        (do
          (aset-double deposit-empty-flow 0 deposits-amount)
          (aset-double deposit-empty-flow (dec term) (- deposits-amount))
          deposit-empty-flow)))


    (defrecord leasing [start-date term periodicity rental opt]
      t/Asset
      (project-cashflow [x]
        (let [dates (u/gen-dates-ary start-date periodicity (inc term))
              rental-flow-list (->> (take term (repeat rental)) (cons 0))
              rental-flow (double-array rental-flow-list)]
          (if (get opt :deposit-balance false)
            (u/gen-table "cashflow"
                         {:dates dates :rental rental-flow :deposit (-leasing-gen-deposit-flow (inc term) opt)})
            (u/gen-table "cashflow"
                         {:dates dates :rental rental-flow}))))


      (project-cashflow [x assump]))

