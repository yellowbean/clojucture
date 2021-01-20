(ns clojucture.local.nowhere
  (:require [clojucture.account :as acc]
            [clojucture.asset :as asset]
            [clojucture.tranche :as trn]
            [clojucture.trigger :as trg]
            [clojucture.pool :as pool]
            [clojucture.util :as u]
            [clojucture.expense :as exp]
            [com.rpl.specter :as s]
            [clojucture.reader.base :as rb]
            [java-time :as jt]
            [clojure.zip :as zip]
            [clojucture.waterfall :as wf]
            [clojucture.spv :as spv])
  )

;; this is a namespace for developing new country specific functions/types
;; once everything have been fully tested, it will be renamed into a new country specific namespace
(defn setup-accounts [d u]
  (->
    (map
      acc/setup-account
      (s/select [:snapshot u :account s/MAP-VALS] d))
    (u/list-to-map-by :name)
    ))

(defn setup-dates [d u]
  (let [sm (get-in d [:dates :stated-maturity])
        cf (get-in d [:dates :cut-off-date])]
    {:stated-maturity (jt/local-date sm)
     :pay-dates       (rb/parsing-dates (str (get-in d [:dates :pay-dates]) "," sm))
     :collect-dates   (cons (jt/local-date cf)
                            (rb/parsing-dates
                              (str
                                (get-in d [:dates :pay-dates]) "," sm)))
     :cut-off-date    (jt/local-date (get-in d [:dates :cut-off-date]))
     :settle-date     (jt/local-date (get-in d [:dates :settle-date]))
     }))

(defn setup-pool [d u]
  (let [asset-list (map
                     #(asset/build-asset % (s/select-one [:snapshot u :pool :asset-type] d))
                     (s/select [:snapshot u :pool :list s/ALL] d))
        cutoff-date (-> (s/select-one [:snapshot u :pool :closing-date] d) (jt/local-date))]
    (pool/->pool asset-list cutoff-date)))

(defn setup-bonds [d u]
  (let [bnd-list (map trn/setup-bond (s/select [:snapshot u :bond s/MAP-VALS] d))
        bnd-name (map keyword (s/select [:snapshot u :bond s/MAP-VALS :info :name] d))]
    (zipmap bnd-name bnd-list)))

(defn setup-triggers [d u]
  (map
    trg/setup-trigger
    (s/select [:snapshot u :trigger s/MAP-VALS] d)))

(defn setup-expenses [d u]
  (->
    (map
      exp/setup-expense
      (s/select [:snapshot u :expense s/MAP-VALS] d))
    (u/list-to-map-by-info :name)
    ))

(defn gen-required-vars [wf]
  "calculate formular "

  )

(defn run-deal [deal assump]
  (let [
        pool (get-in deal [:update :pool])
        current-period (get-in deal [:info :current-period])
        ;coll-dates (->>  (get-in deal [:projection :dates :collect-dates]) (drop current-period ))
        coll-dates (get-in deal [:projection :dates :collect-dates])
        pool-cf (.collect-cashflow pool assump coll-dates)

        pay-date-list (get-in deal [:projection :dates :pay-dates])
        wf (-> (get-in deal [:waterfall]) zip/vector-zip)
        deal-setup (-> (spv/copy-update-to-proj deal)
                       (assoc-in [:projection :period] 1)
                       (assoc-in [:projection :pool-collection] pool-cf)
                       ;(assoc-in [:projection :start-date] )
                       )
        ]
    (loop [dp deal-setup proj-index 0]
      (if (< (get-in dp [:projection :period]) (.rowCount pool-cf))
        (let [pay-date (nth pay-date-list (+ current-period proj-index))
              [update-deal wf-path] (wf/walk-waterfall wf dp pay-date)]
          (recur update-deal (inc proj-index)))
        dp)
      )
    )
  )