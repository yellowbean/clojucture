(ns clojucture.reader.cn
  (:require
    [clojucture.asset :as asset]
    [clojucture.assumption :as assump]
    [clojucture.account :as account]
    [clojucture.expense :as exp ]
    [clojucture.util :as u]
    [clojucture.tranche :as b]
    [clojucture.account :as a]
    [clojucture.pool :as p]
    [clojucture.spv :as spv ]
    [clojure.java.io :as io]
    [clojure.core.match :as m]
    [java-time :as jt]
    )
  (:use [ dk.ative.docjure.spreadsheet ])
  (:import
    [tech.tablesaw.api Table Row DoubleColumn DateColumn StringColumn BooleanColumn]
    [java.util Locale]
    [java.time ZoneId]
    [tech.tablesaw.io.csv CsvReader CsvReadOptions$Builder]
    )
  )

(defn to-ld [ x ]
  (-> x (.toInstant) (.atZone (ZoneId/systemDefault)) (.toLocalDate) ) )

;; China spreadsheet loader
(defn cn-setup-bond [ bm settle-date]
  (let [ fbm bm ]
    (m/match fbm
     { :name bid :balance bal :rate r :type "过手"}
             (b/->sequence-bond {:name bid :settle-date settle-date} bal r [] settle-date 0 0 )
     :else nil
     )
    )
  )

(defn cn-setup-fee [ fm end-date]
  (m/match fm
     {:name n :amount a :start-date s :interval i}
           (exp/->recur-expense {:name n :amount a :period i :start-date (to-ld s) :end-date end-date } [] 0)
     {:name n :amount a}
           (exp/->amount-expense {:name n} [] nil a )
     {:name n :rate r :base b}
           (exp/->pct-expense-by-amount {:name n :base b :pct r} [] nil 0 )
     :else nil
    )
  )

(defn cn-setup-wf [ dist-action ]
  (m/match dist-action
   {:cond cons :source source :target target :opt opt}
     "D"
   {:source source :target target :opt opt}
     {:source (keyword source) :target (keyword target) :opt (keyword opt)}
   {:source source :target target }
     {:source (keyword source) :target (keyword target)}


    :else nil
     )
  )



(defn cn-setup-pool [ a ]
  (m/match a
   {:originate-date od :maturity-date md :term term :closing-date cd :current-balance obalance :rate r :int-feq freq
    :int-interval int-interval :first-pay first-pay :remain-term rm}
           (asset/->loan
             {:start-date (to-ld od) :term term :rate r :balance obalance
              :periodicity (jt/months 3) :first-pay (to-ld first-pay) :maturity-date (to-ld md)}
              obalance rm r nil )


    :nil ) )

(defn cn-setup-acc [ a ]
    (m/match a
             {:name n :type t :balance b} (account/->account (keyword n) (keyword t) b [])
             :else nil
             )
  )

(defn cn-load-model! [ wb-path ]
  (let [ wb (load-workbook wb-path)
        info-s (select-sheet "info" wb)
        [ closing-date settle-date first-payment-date stated-maturity ]
          (map #(to-ld (:date %) ) (select-columns {:A :name :B :date} info-s))

        [ h & assets ]
          (row-seq (select-sheet "pool" wb))

        pool-s (select-sheet "pool" wb)
        asset-list (map cn-setup-pool
                  (subvec (select-columns
                            {:B :originate-date :C :maturity-date :D :closing-date :F :current-balance :G :rate
                             :H :int-feq :I :int-interval :J :term :K :remain-term :L :first-pay}

                            pool-s) 1))

        bond-s (select-sheet "bond" wb)
        bonds (map #(cn-setup-bond % settle-date )
                   (rest (select-columns {:A :name :B :balance :C :rate :D :feq  :E :type } bond-s)))

        fee-s (select-sheet "fee" wb)
        fees-oneoff (map #(cn-setup-fee % closing-date) (subvec (select-columns {:A :name :B :amount} fee-s) 2 ))
        fees-base (map #(cn-setup-fee % closing-date) (subvec (select-columns {:D :name :E :rate :F :base} fee-s) 2 ))
        fees-recur (map #(cn-setup-fee % closing-date) (subvec (select-columns {:H :name :I :amount :J :start-date :K :interval } fee-s) 2 ))

        waterfall-s (select-sheet "waterfall" wb)
        wf-norm (map cn-setup-wf
                     (subvec (select-columns {:A :cond :B :source :C :target :D :opt } waterfall-s) 2 ))

        wf-default (map cn-setup-wf
                     (subvec (select-columns {:F :cond :G :source :H :target :I :opt } waterfall-s) 2 ))

        account-s (select-sheet "account" wb)
        accounts (->> (map cn-setup-acc
                      (subvec (select-columns {:A :name :B :type :C :balance} account-s) 1 ))
                      (u/build-map-from-field :name))
        assump-s (select-sheet "assumption" wb)
        assump-prepay nil
        assump-default nil
        ]
    (spv/build-deal
      {:info {
         :dates {
          :closing-date closing-date :first-collect-date (jt/local-date 2017 6 30) :collect-interval :Q
          :stated-maturity stated-maturity :first-payment-date (jt/local-date 2017 6 30) :payment-interval :Q
          :delay-days 20 }
         :waterfall {
                     :normal  wf-norm
                     :default wf-default
                     }
         :accounts accounts
         :country :China
         :deposit-mapping {:principal :账户P  :interest :账户I}
        }
      :status {
        :update-date (jt/local-date 2017 4 30)
        :pool (p/->pool asset-list (jt/local-date 2018 1 1 ))
        :bond bonds
        :expense [fees-oneoff fees-base fees-recur]
        :account accounts
         }}
      )
    )
  )


