(ns clojucture.builder_test
  (:require [clojure.test :refer :all]
            [clojucture.builder :as br]
            [clojucture.local.nowhere :as nw]))

(def test-deal
  {
   :info     {
              :current-period 5
              }
   :dates    {:settle-date     "2017-11-14"
              :pay-dates       "2018-02-26M"
              :stated-maturity "2049-01-01"
              :cut-off-date "2017-06-01"
              }
   :snapshot {
              "2018-05-26" {:info    {:current-period 5}
                            :account {
                                      :cash-acc {:name :cash-acc :balance 0}
                                      :prin-acc {:name :prin-acc :balance 0}
                                      :int-acc  {:name :int-acc :balance 0}
                                      :exp-acc  {:name :exp-acc :balance 0}
                                      }
                            :pool    {
                                      :closing-date "2017-11-20"
                                      :asset-type   :mortgage
                                      :list         [
                                                     {:original-balance 1250000000 :current-balance 1010000000 :annual-rate 0.049 :originate-date "2013-12-25"
                                                      :pay-dates        "2014-01-25M" :original-term 240 :remain-term 200}]}

                            :bond    {
                                      :A      {:type :sequential
                                               :info {:name "A" :original-balance 2550000000 :expected-date "2019-02-26" :stated-maturity "2047-4-26"} :balance 2550000000 :rate 0.04
                                               :last-payment-date {:principal "2018-04-15" :interest "2018-04-15"} :principal-loss 0 :interest-arrears 0  :stmts []}
                                      :Junior {:type :sequential
                                               :info {:name "Junior" :original-balance 1299999999.72 :expected-date "2047-04-26" :stated-maturity "2047-4-26"} :balance 1299999999.72 :rate 0.07
                                               :last-payment-date {:principal "2018-04-15" :interest "2018-04-15"} :principal-loss 0 :interest-arrears 0 :stmts []}
                                      }
                            :expense {
                                      :trustee-fee {:name :trustee-fee :info {:base [:projection :pool :sum-current-balance] :annual-pct 0.005} :arrears 0 :last-paid-date "2018-04-26"}
                                      }

                            :trigger {
                                      :event-of-default {:name :eod :cond [:pool-cumulative-default-rate :> 0.03] :status false}}
                            }
              }

   :waterfall
             [
              {:from :current-collection :to :cash-acc :fields ["interest" "principal" "prepayment"]}
              {:from :cash-acc :to :exp-acc :expense :trustee-fee}
              {:from :cash-acc :to :int-acc :interest :A}
              {:from :cash-acc :to :int-acc :interest :Junior}
              {:from :cash-acc :to :prin-acc :principal :A}
              {:from :cash-acc :to :prin-acc :principal :Junior}
              ]

   })


(deftest tLoadDeal
  (let [d (br/load-deal test-deal) ]
    (prn (:update d)) ;TBD
    )
  )
