(ns clojucture.local_cn
  (:require
    [clojucture.asset :as asset]
    [clojucture.account :as acc]
    [clojucture.spv :as d]
    [clojucture.bond :as b]
    [clojucture.pool :as p]
    [clojucture.util :as u]
    [java-time :as jt]
    [clojure.core.match :as m])
  (:use [clojure.core.match.regex])
  (:import java.util.Arrays)
  )




(defn setup-accounts [ d ]
  (loop [ accs [] accs-to-add (:账户 d)]
    (if (nil? accs-to-add)
      accs
      (recur
        (conj accs
              (m/match (first accs-to-add)
                {:名称 acc-name } (acc/->account acc-name nil 0 [])
                :else nil))
        (next accs-to-add)))
  ))


(comment
  (defn setup-dates [ d ]
    (let [cut-off-date (jt/local-date (get-in d [:日期 :初始起算日]))
          stated-maturity-date (jt/local-date (get-in d [:日期 :法定到期日])) ]
      {
       :cut-off-date    cut-off-date
       :stated-maturity stated-maturity-date
       :pay-dates       (u/parsing-dates (get-in d [:日期 :支付日]) stated-maturity-date)
       :int-dates       (u/parsing-dates (get-in d [:日期 :计息日]) stated-maturity-date)
       :calc-dates      (u/parsing-dates (get-in d [:日期 :计算日]) stated-maturity-date)
       :dist-dates      (u/parsing-dates (get-in d [:日期 :信托分配日]) stated-maturity-date)
       }
      )
    )

  (defn parsing-assets [asset]
    (m/match asset
             {:初始面额 orig_balance :当前面额 current_balance :年利率 annual_rate :摊销 amort_type
              :支付日期 payment_dates :初始期限 orig_term :当前期限 remain_term :初始日 start_date}
             (asset/->mortgage (jt/local-date start_date))

             )

    )



  (defn setup-pool [d]
    (let [asset-list (map #(parsing-assets %) (get-in d [:资产池 :资产清单]))]



      {:asset-list asset-list}
      )
    )

  (defn setup-triggers [d]

    )

  (defn setup-expense [d]

    )

  (defn setup-bond [d]

    )






  (defrecord china-bank-deal [deal-info opt status update-date]
    d/Deal
    (run-assets [x assump]
      (let [
            ]
        )
      )
    (run-triggers [x]

      )
    (run-bonds [x assump]

      )
    )



  (defn build-deal [deal-type deal-structure]
    (let [
          update (get-in deal-structure [:meta :update-date])
          accounts (setup-accounts deal-structure)
          dates (setup-dates deal-structure)
          pool (setup-pool deal-structure)
          bond (setup-bond deal-structure)
          triggers (setup-triggers deal-structure)
          expense (setup-expense deal-structure)
          ]
      (case deal-type
        ;:bank-deal (china-bank-deal. nil nil nil )

        )
      )
    )
  )