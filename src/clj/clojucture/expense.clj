(ns clojucture.expense
  (:require
    [clojucture.account :as acc]
    [clojucture.spv :as spv]
    [java-time :as jt]
    [clojucture.util :as u]
    [clojure.core.match :as m]
    [com.rpl.specter :as s])
  (:import [java.time LocalDate Period]
           )
  )


(defprotocol pExpense
  (receive [x d amount])
  )

(defn cal-due-expense [deal exp calc-date]
  "calculate due expense during the projection"
  (m/match (into {} exp)
           {:info {:base epr :pct pct} :arrears ars}
           (let [deal-val (spv/query-deal deal epr)]
             (-> (* deal-val pct) (+ ars)))

           {:info {:base epr :annual-pct pct :day-count dc} :last-paid-date lpd :arrears ars}
           (let [deal-val (spv/query-deal deal epr)
                 ;_ (prn "exr " epr )
                 ;_ (prn "deal-val" deal-val)
                 ]
             (-> (u/get-period-rate (Period/between lpd calc-date) pct dc)
                 (* deal-val) (+ ars)))

           {:info {:name x} :balance bal}
           bal

           {:info {:start-date sd :interval intv :balance bal} :arrears ars :last-paid-date lpd}
           (let [fee-pay-dates (u/gen-dates-range sd intv)
                 missed-periods (->>
                                  (drop-while #(jt/before? % lpd) fee-pay-dates)
                                  (take-while #(jt/before? % calc-date)))
                 unpaid-balance (* bal (count missed-periods))
                 ]
             (+ ars bal unpaid-balance))

           :else (throw (Exception. "Not match due expense formula")))
  )

(defn pay-expense-at-base
  [^LocalDate d acc expense base]
  (let [due-amount (.cal-due-amount expense d base)
        new-acc (.try-withdraw acc d (:info acc) due-amount)
        paid-amount (Math/abs (:amount (.last-txn new-acc)))
        new-arrears (- due-amount paid-amount)]
    [
     new-acc
     (-> expense
         (assoc :arrears new-arrears)
         (assoc :last-paid-date d))
     ])
  )



(defn -pay-expense-amount [^LocalDate d acc expense amt]
  "actual pay an expense by certain amount from an account"
  (let [draw-amount (min (.balance acc) amt)
        new-acc (.try-withdraw acc d (:name acc) draw-amount)]
    [new-acc (.receive expense d draw-amount)]
    )
  )


(defn- apply-rule [due-amount opt]
  (m/match opt
           {:upper-limit-amount r}
           (min r due-amount)
           {:upper-limit-pct p}
           (* due-amount p)
           :else due-amount
           ))


(defn pay-expense [deal ^LocalDate d source-acc expense opt]
  "pay a single expense ; return [ account , expense ]"
  (let [due-amount (cal-due-expense deal expense d)
        adj-due (apply-rule due-amount opt)
        avail-amount (.balance source-acc)
        amt (min avail-amount adj-due)
        new-acc (.withdraw source-acc d (get-in expense [:info :name]) amt)]
    [new-acc (.receive expense d amt)]
    ))



(defn- distribute-amount-to-expenses [^Double amount ^LocalDate d due-factors source-acc exp-map]
  (let [each-payment (map #(* amount %) due-factors)]
    (loop [ep each-payment exp-m exp-map paying-acc source-acc rexp-map {}]
      (if-let [[ek ev] (first exp-m)]
        (let [[acc-after-pay exp-after-pay] (-pay-expense-amount d paying-acc ev (first ep))]
          (recur (next ep) (next exp-m) acc-after-pay (assoc rexp-map ek exp-after-pay)))
        [paying-acc rexp-map]
        )
      )
    )
  )



(defn pay-expenses-pr
  ([deal ^LocalDate d source-acc exp-map opt]
   "pay a list of expenses on pro-rata basis, with optional cap; return [ account, expense-map ]  "
   (let [due-amt-list (map #(cal-due-expense deal % d) (vals exp-map))
         total-due-amount (reduce + due-amt-list)
         total-due-factors (map #(/ % total-due-amount) due-amt-list)
         avail-amount (.balance source-acc)
         total-payment (min avail-amount (apply-rule total-due-amount opt))]
     (distribute-amount-to-expenses total-payment d total-due-factors source-acc exp-map)))

  ([deal ^LocalDate d source-acc exp-map]
   (pay-expenses-pr deal d source-acc exp-map nil)))



(defrecord pct-expense-by-amount
  ;"Expense type that due amount is annualized percentage of the base, i.e trustee fee"
  [info stmt ^LocalDate last-paid-date ^Double arrears]
  pExpense
  (receive [x d amount]
    (let [pay-to-arrears (min arrears amount)
          pay-to-expense (- amount pay-to-arrears)]
      (-> x
          (assoc :arrears (- arrears pay-to-arrears))
          (assoc :last-paid-date d)
          (assoc :stmt
                 (conj stmt
                       (acc/->stmt d nil :expense pay-to-expense nil)
                       (acc/->stmt d nil :expense-arrears pay-to-arrears nil)))
          )
      )
    )
  )

(defrecord pct-expense-by-rate
  ;"Pct expense type that is percentage of the base, i.e VAT"
  [info stmt ^LocalDate last-paid-date ^Double arrears]
  pExpense
  (receive [x d amount]
    (let [pay-to-arrears (min arrears amount)
          pay-to-expense (- amount pay-to-arrears)]
      (-> x
          (assoc :arrears (- arrears pay-to-arrears))
          (assoc :last-paid-date d)
          (assoc :stmt
                 (conj stmt
                       (acc/->stmt d nil :expense pay-to-expense nil)
                       (acc/->stmt d nil :expense-arrears pay-to-arrears nil)))
          )
      )
    )
  )

(defrecord amount-expense
  [info stmt ^LocalDate last-paid-date ^Double balance]
  pExpense
  (receive [x d amount]
    (if (> amount balance)
      (throw (Exception. "Expense paid over balance"))
      (-> x
          (assoc :balance (- balance amount))
          (assoc :last-paid-date d)
          (assoc :stmt (conj stmt (acc/->stmt d nil :expense amount nil)))
          )
      )
    )
  )

(defrecord recur-expense
  [info stmt ^Double arrears]
  pExpense
  (receive [x d amount]
    (let [pay-to-arrears (min arrears amount)
          pay-to-expense (- amount pay-to-arrears)]
      (-> x
          (assoc :arrears (- arrears pay-to-arrears))
          (assoc :stmt (conj stmt
                             (acc/->stmt d nil :expense pay-to-expense nil)
                             (acc/->stmt d nil :expense-arrears pay-to-arrears nil)))

          )
      )
    )
  )


(defn setup-expense [x]
  ;(prn "matching exp: " x)
  (m/match x
           {:name n :info {:base bse :annual-pct pct} :last-paid-date pd :arrears ars}
           (->pct-expense-by-amount {:name n :base bse :annual-pct pct :day-count :ACT_365} [] (jt/local-date pd) ars)

           {:name n :balance v :last-paid-date pd}
           (->amount-expense {:name n} [] (jt/local-date pd) v)

           {:name n :info {:base bse :pct pct} :last-paid-date pd :arrears ars}
           (->pct-expense-by-rate {:name n :base bse :pct pct} [] (jt/local-date pd) ars)

           {:name n :info {:start-date sd :interval intv} :last-paid-date pd :arrears ars}

           :else (throw (Exception. "Not matching Expense")))
  )


