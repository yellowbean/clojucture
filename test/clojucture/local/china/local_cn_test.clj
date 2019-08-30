(ns clojucture.local.china.local_cn_test
  (:require [clojure.test :refer :all]
            [java-time :as jt]
            [clojucture.local.china.local_cn :as cn]
            [clojucture.spv :as spv]
            [medley.core :as mc]
            [clojucture.util :as u])
  ;(:import (clojucture RateAssumption))
  )


(def jy-info {
              :日期       {:初始起算日 "2017-11-14"
                         :支付日   "2018-02-26M"
                         :计息日   "2018-01-23,2018-02-26M"
                         :计算日   "2018-01-31ME"
                         :信托分配日 "2018-02-26M-6WD"
                         :法定到期日 "2049-04-26"}
              :snapshot {
                         "2018-05-26" {
                                       :账户   {
                                              :收入分账户       {:名称 :收入分账户 :余额 0}
                                              :本金分账户       {:名称 :本金分账户 :余额 0}
                                              :分配本金账户      {:名称 :分配本金账户 :余额 0}
                                              :分配利息账户      {:名称 :分配利息账户 :余额 0}
                                              :分配费用账户      {:名称 :分配费用账户 :余额 0}
                                              :服务转移和通知储备账户 {:名称 :服务转移和通知储备账户 :余额 0}
                                              :税收储备账户      {:名称 :税收储备账户 :余额 0}

                                              }
                                       :资产池  {
                                              :封包日 "2017-11-20"
                                              :类型  :住房按揭
                                              :资产清单
                                                   [
                                                    {:初始面额 1250000000 :当前面额 1010000000 :年利率 0.049 :摊销 "等额本息" :初始日 "2013-12-25"
                                                     :支付日期 "2014-01-25M" :初始期限 240 :当前期限 200}
                                                    {:初始面额 1250000000 :当前面额 1010000000 :年利率 0.049 :摊销 "等额本息" :初始日 "2013-12-25"
                                                     :支付日期 "2014-01-25M" :初始期限 240 :当前期限 200}]
                                              }

                                       :债券   {
                                              :A-1 {:简称    "A-1" :初始面额 2550000000 :当前余额 2550000000 :执行利率 0.04 :预期到期日 "2019-02-26" :法定到期日 "2047-4-26"
                                                    :上次付款日 {:principal "2018-04-15" :interest "2018-04-15"}}
                                              :A-2 {:简称    "A-2" :初始面额 6150000000 :当前余额 6150000000 :执行利率 0.05 :预期到期日 "2023-03-26" :法定到期日 "2047-4-26"
                                                    :上次付款日 {:principal "2018-04-15" :interest "2018-04-15"}}
                                              :次级  {:简称    "次级" :初始面额 1299999999.72 :当前余额 1299999999.72 :预期到期日 "2047-04-26" :法定到期日 "2047-4-26"
                                                    :上次付款日 {:principal "2018-04-15" :interest "2018-04-15"}}
                                              }

                                       :费用   {
                                              :受托机构报酬   {:name :受托机构报酬 :year-rate 0.0005 :arrears 0 :last-paid-date "2018-04-26"}
                                              :资金保管机构报酬 {:name :资金保管机构报酬 :year-rate 0.0005 :arrears 0 :last-paid-date "2018-04-26"}
                                              :支付代理机构报酬 {:name :支付代理机构报酬 :year-rate 0.0005 :arrears 0 :last-paid-date "2018-04-26"}
                                              :评级机构报酬   {:name :评级机构报酬 :balance 5000 :last-paid-date "2018-04-26"}
                                              :审计师报酬    {:name :审计师报酬 :balance 5000 :last-paid-date "2018-04-26"}
                                              :服务机构服务报酬 {:name :服务机构服务报酬 :year-rate 0.001 :arrears 0 :last-paid-date "2018-04-26"}
                                              ;{:name :后备贷款服务机构报酬 :year-rate 0.0000 :arrears 0}
                                              :法律顾问报酬   {:name :法律顾问报酬 :balance 5000 :last-paid-date "2018-04-26"}
                                              :增值税      {:name :增值税 :base-rate 0.0326 :last-paid-date "2018-04-26"}
                                              :预计通知费用   {:name :预计通知费用 :balance 50000 :last-paid-date "2018-04-26"}
                                              :预计转移费用   {:name :预计转移费用 :balance 50000 :last-paid-date "2018-04-26"}
                                              :报销       {:name :报销 :balance 100000 :last-paid-date "2018-04-26"}
                                              }

                                       :风险事件 {
                                              :违约事件   {:name :违约事件 :cond [:资产池违约率 :大于 0.03]}
                                              :加速清偿事件 {:name :加速清偿事件 :cond [:资产池违约率 :大于 0.01]}
                                              }
                                       }
                         }

              :分配方式     {
                         :违约前 [
                               {:from :收入回收款 :to :收入分账户 :fields ["interest"]}
                               {:from [:本金分账户 :服务转移和通知储备账户 :税收储备账户] :to :收入分账户}
                               {:from :收入分账户 :to :税收储备账户 :expense :增值税}
                               {:from    :收入分账户 :to :分配费用账户
                                :expense [:法律顾问报酬 :会计师报酬 :初始评级费用 :承销报酬 :财务顾问报酬 :登记托管服务费]
                                }
                               {:from :收入分账户 :to :服务转移和通知储备账户 :expense :预计通知费用}
                               {:from :收入分账户 :to :服务转移和通知储备账户 :expense :预计转移费用}

                               {:from    :收入分账户 :to :分配费用账户
                                :expense [:受托机构报酬 :资金保管机构报酬 :支付代理机构报酬 :评级机构报酬 :审计师报酬 :后备贷款服务机构报酬]}
                               {:from    :收入分账户 :to :分配费用账户
                                :expense :报销 :limit :优先支出上限
                                }
                               {:from :收入分账户 :to :分配费用账户 :expense :服务机构服务报酬 :with-limit-percentage 0.5}
                               {:from :收入分账户 :to :分配利息账户 :interest [:A-1.DueInt :A-2.DueInt]}
                               {:from :收入分账户 :to :分配费用账户 :expense :服务机构服务报酬}
                               {:if :加速清偿事件
                                :breached
                                    [{:from :收入分账户 :to :本金分账户}]
                                :unbreach
                                    [
                                     {:from :收入分账户 :to :本金分账户 :formula :a+b+c-d} ;a+b+c-d
                                     {:from :收入分账户 :to :分配费用账户 :expense :报销}
                                     {:from :收入分账户 :to :本金分账户}]
                                }
                               ; 本金账户
                               {:from :本金回收款 :to :本金分账户 :fields ["principal"]}
                               ;{:from :本金分账户 :to :收入分账户 :amount nil} ;用于足额支付信托合同ai to a vii
                               {:from :收入分账户 :to :本金分账户}    ; 收入分账户
                               {:if :加速清偿事件
                                :breached
                                    [{:from :本金分账户 :to :分配本金账户 :principal [:A-1.Balance :A-2.Balance]}]
                                :unbreach
                                    [{:from :本金分账户 :to :分配本金账户 :principal :A-1.Balance}
                                     {:from :本金分账户 :to :分配本金账户 :principal :A-2.Balance}]
                                }
                               {:from :收入分账户 :to :分配本金账户 :principal :次级.Balance}
                               {:from :收入分账户 :to :分配利息账户 :interest :次级.Interest}]
                         :违约后 [
                               {:from [:本金分账户 :收入分账户 :服务转移和通知储备账户 :税收储备账户] :to :信托收款账户}
                               {:from :信托收款账户 :to :税收储备账户 :amount :增值税}
                               {:from   :信托收款账户 :to :分配费用账户
                                :amount [:法律顾问报酬 :会计师报酬 :初始评级费用 :承销报酬 :财务顾问报酬 :登记托管服务费]}
                               {:from   :信托收款账户 :to :分配费用账户
                                :amount [:受托机构报酬 :资金保管机构报酬 :支付代理机构报酬 :评级机构报酬 :审计师报酬 :后备贷款服务机构报酬 :报销]}
                               {:from :信托收款账户 :to :分配利息账户 :amount [:A-1.DueInt :A-2.DueInt]}
                               {:from :信托收款账户 :to :分配本金账户 :amount [:A-1.Balance :A-2.Balance]}
                               {:from :信托收款账户 :to :分配本金账户 :amount :次级.Balance}
                               {:from :收入分账户 :to :分配利息账户 :amount :次级.Interest}
                               ]
                         :终止  [
                               {:from :信托账户 :to :税收储备账户 :amount :增值税}
                               {:from :信托账户 :to :分配费用账户 :amount :清算费用}
                               {:from   :信托收款账户 :to :分配费用账户
                                :amount [:受托机构报酬 :资金保管机构报酬 :支付代理机构报酬 :评级机构报酬 :审计师报酬 :后备贷款服务机构报酬 :报销]}
                               {:from :信托收款账户 :to :分配利息账户 :amount [:A-1.DueInt :A-2.DueInt]}
                               {:from :信托收款账户 :to :分配本金账户 :amount [:A-1.Balance :A-2.Balance]}
                               {:from :信托收款账户 :to :分配本金账户 :amount :次级.Balance}
                               {:from :收入分账户 :to :分配利息账户 :amount :次级.Interest}

                               ]
                         }

              })

(deftest tAcc
  (let [accounts (cn/setup-accounts jy-info "2018-05-26")]
    (is (= (:name (second (first accounts))) :收入分账户))
    (is (= (:balance (second (first accounts))) 0))
    (is (= (count accounts) 7))
    )
  )

(deftest tAsset
  (let [assets (cn/setup-assets jy-info "2018-05-26")]
    (is (= 2 (count assets)))
    ))

(deftest tPool
  (let [pool (cn/setup-pool jy-info "2018-05-26")]
    (is (= 2 (count (:assets pool))))
    (is (= (jt/local-date 2017 11 20) (:cutoff-date pool)))
    ))

(deftest tDates
  (let [dates-info (cn/setup-dates jy-info)]
    (is (= (second (:dist-dates dates-info)) (jt/local-date 2018 3 16)))
    ))

(deftest tFees
  (let [fees (cn/setup-expenses jy-info "2018-05-26")]
    (is (= (count fees) 11))
    )
  )


(deftest tTrigger
  (let [trgs (cn/setup-triggers jy-info "2018-05-26")]
    (is (= (count trgs) 2))
    ))

(deftest tBond
  (let [bnds (cn/setup-bonds jy-info "2018-05-26")]
    (is (= (count bnds) 3)))
  )


(deftest tDealFunc
  (let [snps (spv/list-snapshots jy-info)
        snps-names (keys snps)]
    (is (= (first snps-names) "2018-05-26"))
    ))




(deftest tLoadBankDeal
  (let [jy-bank (cn/load-deal jy-info "2018-05-26")
        pool-assump {:prepayment nil :default nil}

        pl (get-in jy-bank [:update :资产池])
        col-int (get-in jy-bank [:projection :dates :calc-dates])
        pool-cf (cn/run-pool pl pool-assump col-int)

        ;finish-run-deal (cn/run-deal jy-bank pool-assump)
        calc-dates (get-in jy-bank [:projection :dates :calc-dates])
        pay-dates (get-in jy-bank [:projection :dates :pay-dates])
        ]
    ;(is (= (count calc-dates) (inc (count pay-dates))))
    ;(u/out-lists "d-tie.out.txt" calc-dates pay-dates)

    (is (=  (first pay-dates) (jt/local-date 2018 2 26)))
    (is (=  (first calc-dates) (jt/local-date 2017 11 14)))
    ;(prn (get-in finish-run-deal [:projection :bond :A-2]))
    )
  )

(deftest tLoadLatestSnapshot
  (let [dummy-deal {:snapshot {"2018-09-09" :sep2018 "2020-05-05" :may2020 "2019-01-01" :jan2019}}
        latest (-> (sort-by jt/local-date (keys (:snapshot dummy-deal))) (last))
        ]
    (is (= latest "2020-05-05")))

  (let [jy-bank (cn/load-deal jy-info)
        update-date-loaded (get-in jy-bank [:update :info :update-date])]
    (is (= update-date-loaded "2018-05-26"))
    )

  )
