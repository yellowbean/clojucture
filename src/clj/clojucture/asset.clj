(ns clojucture.asset
  (:require [java-time :as jt]
            [clojucture.util :as u]
            [clojucture.assumption :as a]
            [clojucture.util-cashflow :as cfu]
            [clojure.core.match :as m])
  (:import
    [java.time LocalDate Period]
    [org.apache.commons.lang3 ArrayUtils]
    (clojucture RateAssumption)))


(defprotocol Asset
  (project-cashflow [x] [x assump] "project cashflow with/out assumption")
  )

(defn -loan-gen-prin [term balance opt]
  "Generate principal vector for loan with option of schedule principal"
  (let [principal-flow (double-array term 0)]
    (if (get opt :schedule-principal false)
      (doseq [[i pmt] (opt :schedule-principal)]
        (aset-double principal-flow i pmt)))
    (aset-double principal-flow (dec term) balance)
    principal-flow))


(defn -gen-interest-rate [info d assump]
  (m/match info
           {:float-info fi}
           (a/apply-curve (:index-curves assump) fi)
           :else
           nil))


(defn cal-period-pmt
  "Calculate the period payment for a mortgage"
  [ ^Double balance ^Integer n-per ^Double period-rate]
  (let [c (Math/pow (+ 1 period-rate) n-per)
        a (/ (* period-rate c) (- c 1))]
    (* a balance)))

;(defn get-current-pmt [history info]
;  (if-let [reset-info (:last-pmt-reset history)]
;    (period-pmt (:balance reset-info) (:term reset-info) (:period-rate reset-info))
;    (period-pmt (:balance info) (:term info) (:period-rate info))))

(defrecord mortgage [info history balance period-rate remain-term opt]
  Asset
  (project-cashflow [x]
    (let [
          {start_date :start-date periodicity :periodicity term :term} info
          date-rng (u/gen-dates start_date periodicity (inc term))
          date-rng-ary (u/dates [(first date-rng) (last date-rng)])
          dz (u/ldoubles [0.0])
          nil-assumption {:prepayment (RateAssumption. "P" date-rng-ary dz)
                          :default    (RateAssumption. "P" date-rng-ary dz) :recovery [nil nil]}]
      (.project-cashflow x nil-assumption)))

  (project-cashflow [x assump]
    (let
      [{start_date :start-date periodicity :periodicity
        term       :term orig-balance :balance} info
       ;{last-paid-date :last-paid-date} history
       ;{stl-date :settle-date} assump

       ;amortized-factor (/ balance orig-balance)
       ;orig-period-pmt (cal-period-pmt orig-balance term period-rate)  ; (get-current-pmt history info)            ; period payment
       current-period-pmt (cal-period-pmt balance remain-term period-rate)

       dates (u/gen-dates start_date periodicity (inc term))
       remain-dates (subvec (vec dates) (- term remain-term) (inc term))
       even-principal-pmt (/ balance term)

       {projected-prepayment-rate :prepayment-curve
        projected-default-rate    :default-curve} (a/gen-assump-curve remain-dates assump)
       ;projected-interest-rates (-gen-interest-rate info nil assump)
       ]
      (loop [payment-dates remain-dates paid-dates []
             bal-list [] prin-list [] int-list []
             ppy-bal-list [] def-bal-list []
             last-bal balance
             ppy-rate projected-prepayment-rate def-rate projected-default-rate
             ]
        (if (or (empty? payment-dates) (< last-bal 0.01))
          (u/gen-cashflow "cashflow"
                          [{:name :dates :type :date :values paid-dates}
                           {:name :balance :type :balance :values bal-list}
                           {:name :principal :type :cash :values prin-list}
                           {:name :interest :type :cash :values int-list}
                           {:name :prepayment :type :cash :values ppy-bal-list}
                           {:name :default :type :balance :values def-bal-list}])
          (let [
                f-bal last-bal                              ; beginning balance
                int-amount (* f-bal period-rate)            ; current interest amount
                prin-amount (- current-period-pmt int-amount)       ; principal amount
                ppy-bal (* f-bal (first ppy-rate))          ;prepayment balance
                bal-after-ppy (- f-bal ppy-bal)             ; balance after prepayment
                def-bal (* bal-after-ppy (first def-rate))  ;default balance
                bal-after-def (- bal-after-ppy def-bal)     ; balance after default
                n-bal (- bal-after-def prin-amount ppy-bal def-bal) ; ending balance
                ]
            (recur
              (rest payment-dates)
              (conj paid-dates (second payment-dates))
              (conj bal-list n-bal)
              (conj prin-list prin-amount)
              (conj int-list int-amount)
              (conj ppy-bal-list ppy-bal)
              (conj def-bal-list def-bal)
              n-bal
              (next ppy-rate)
              (next def-rate)
              )
            )))

      )
    ))




(defrecord float-mortgage [^LocalDate start-date periodicity ^Integer term ^Integer remain-term ^Double current-rate ^Double current-balance day-count float-info opt]
  Asset
  (project-cashflow [x]
    nil)


  (project-cashflow [x assump]
    (let [term (inc term)
          dates (u/gen-dates-ary start-date periodicity term)
          bal (double-array term 0)
          _ (aset-double bal 0 current-balance)
          prin (double-array term 0)
          int (double-array term 0)
          even_principal_pmt (/ current-balance term)
          reset-dates (:reset-dates float-info)
          future-rates-of-asset (a/apply-curve (:indexes assump) float-info)
          period-annual-rate-vector (u/gen-float-period-rates dates future-rates-of-asset)
          period-rate-vector (map #(u/get-period-rate periodicity % day-count) period-annual-rate-vector)]

      (doseq [i (range 1 term)]
        (let [last_period_balance (aget bal (dec i))
              period_interest (* last_period_balance (nth period-rate-vector i))
              period_pmt (cal-period-pmt current-balance (inc term) (nth period-rate-vector i))
              period_principal (if (false? (get opt :even-principal false))
                                 (- period_pmt period_interest)
                                 even_principal_pmt)]
          (aset-double bal i (- last_period_balance period_principal))
          (aset-double prin i period_principal)
          (aset-double int i period_interest)))


      (u/gen-table "cashflow"
                   {:dates    dates :balance bal :principal prin
                    :interest int :rate (double-array period-rate-vector)}))))










(defrecord loan [info current-balance remain-term current-rate opt]
  Asset
  (project-cashflow [x]
    (let [{first-pay :first-pay maturity-date :maturity-date} info
          def-a (a/gen-pool-assump-df :cdr [0 0] [first-pay maturity-date])
          ppy-a (a/gen-pool-assump-df :cpr [0 0] [first-pay maturity-date])
          empty-assumption {:prepayment ppy-a :default def-a}
          ]
      (.project-cashflow x empty-assumption)))

  (project-cashflow [x assump]
    (let [{start-date    :start-date first-pay :first-pay periodicity :periodicity
           maturity-date :maturity-date term :term rate :rate balance :balance} info
          payment-dates (->
                          (cons start-date
                                (take term (u/gen-dates-range first-pay periodicity)))
                          (vec) (conj maturity-date))
          rest-payment-dates (subvec (vec payment-dates) (- term remain-term))
          next-payment-date (first rest-payment-dates)
          last-payment-date (nth (vec payment-dates) (- term remain-term 2))
          {ppy-assump :prepayment def-assump :default} assump
          ppy-assump-proj (a/gen-asset-assump ppy-assump rest-payment-dates)
          def-assump-proj (a/gen-asset-assump def-assump rest-payment-dates)
          ;result column
          projection-periods (count rest-payment-dates)
          bal (double-array projection-periods)
          prin (double-array projection-periods)
          interest (double-array projection-periods)
          prepay (double-array projection-periods)
          default (double-array projection-periods)
          _ (aset-double bal 0 current-balance)
          payment-dates-int (u/gen-dates-interval rest-payment-dates)
          ]
      (loop [pds payment-dates-int ppy-a (seq ppy-assump-proj) def-a (seq def-assump-proj)
             f-bal current-balance p-dates [last-payment-date] idx 1]
        (if (or (nil? pds) (< f-bal 0.01))
          (u/gen-cashflow "CF"
                          {:dates      (u/dates p-dates) :balance bal :principal prin :interest interest
                           :prepayment prepay :default default})
          (let [df-amt (* f-bal (first def-a))              ; default amount
                py-amt (* f-bal (first ppy-a))              ; prepayment amount
                next-balance (- f-bal df-amt py-amt)
                pd (first pds)                              ;; current payment date interval
                int-amount (* f-bal (u/cal-period-rate (first pd) (second pd) current-rate :30_365))
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
  Asset
  (project-cashflow [x]
    (let [
          dates [start-date end-date]
          bal [balance 0]
          prin [0 balance]
          ]
      (u/gen-table
        {:name "cashflow" :dates dates :balance bal :principal prin}

        )))
  (project-cashflow [x assump]

    nil))



(defrecord installments [balance start-date periodicity term period-fee-rate opt]
  Asset
  (project-cashflow [x]
    (let [
          dates (u/gen-dates-ary start-date periodicity (inc term))
          even-principal (/ balance term)
          even-principal-list (->> (take term (repeat even-principal)) (cons 0))
          prin (double-array even-principal-list)
          bal (cfu/gen-end-balance prin balance)
          period-fee (* balance period-fee-rate)
          period-fee-list (->> (take term (repeat period-fee)) (cons 0))
          fee (double-array period-fee-list)]

      (u/gen-table {:name  "cashflow"
                    :dates dates :balance bal :principal prin :installment-fee fee})))


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
  Asset
  (project-cashflow [x]
    (let [dates (u/gen-dates start-date periodicity (inc term))
          rental-flow-list (->> (take term (repeat rental)) (cons 0))]
      (if (get opt :deposit-balance false)
        (u/gen-table {:name "cashflow" :dates dates :rental rental-flow-list :deposit (-leasing-gen-deposit-flow (inc term) opt)})
        (u/gen-table {:name "cashflow" :dates dates :rental rental-flow-list}))))


  (project-cashflow [x assump]))


(defn build-asset [x t]
  "map as input, return a record instance representing the asset"
  (m/match (assoc x :type t)

           {:type          :mortgage :current-balance bal :annual-rate ar :originate-date od :remain-term rt
            :original-term ot :original-balance ob}
           (->mortgage {:start-date (jt/local-date od) :periodicity (jt/months 1)
                        :term ot :balance ob :period-rate (/ ar 12)} nil bal (/ ar 12) rt nil)

           ;(map->mortgage (dissoc x :type))

           :else :not-match-asset
           )

  )