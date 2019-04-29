(ns clojucture.waterfall
  (:require
    [clojure.core.match :as m]
    [clojucture.expense :as exp]
    [clojucture.bond :as bnd])
  )

(defn test-constrain [ d c]
  "Test constrain on deal return a boolean"
  )


(defn execute-instruction [ deal d payment-action ]
  "execute current payment action base one current deal status"
  (let [ { source :source target :target opt :opt }  payment-action
           fee-ks (keys (:fee deal))
           fee-key? (partial contains? fee-ks )
           bond-ks (keys (:bond deal))
           bond-key? (partial contains? bond-ks ) ]
    (if (test-constrain deal (get payment-action :cond true))
      (m/match [ source  target  opt ]
        [ _ (fk :guard fee-key?)  :due ]
          (exp/pay-expense-deal deal d source fk opt)
        [ _ (bk :guard bond-key?) (:or :due-int :due-principal) ]
          (bnd/pay-bond-deal deal d source bk opt)

        :else nil)
      nil)
    )
  )


(defn execute-waterfall [ ws-list deal d ]
  "loop over a list of instructions and execute them in a deal"
  (loop [  wt ws-list update-deal deal ]
    (if-let [ instruction  (first wt) ]
      (recur (next wt) (execute-instruction instruction update-deal d))
      update-deal ) ))



