(ns clojucture.builder
  (:require [clojure.core.match :as m]
            [clojucture.local.china :as cn]
            [clojucture.local.nowhere :as nw]
            [java-time :as jt]

            [clojucture.util :as util]
            [clojure.zip :as zip])
  )



(defn- pick-initializer [ d ]
  (m/match d
           ;; if it is a deal with Chinese char
           {:信息 _ } {:accs cn/setup-accounts :dates cn/setup-dates :pool cn/setup-pool
                     :bonds cn/setup-bonds :triggers cn/setup-triggers :expense cn/setup-expenses}
           ;; if it is a deal with English words
           {:info _ } {:accs nw/setup-accounts :dates nw/setup-dates :pool nw/setup-pool
                                :bonds nw/setup-bonds :triggers nw/setup-triggers :expense nw/setup-expenses}
           )


  )

(defn load-deal
  ([deal-info u]
   (let [; initialized deal from a clojure map

         init-fns (pick-initializer deal-info)

         accounts ((:accs init-fns) deal-info u)
         dates ((:dates init-fns) deal-info u)
         pool ((:pool init-fns) deal-info u)
         bond ((:bonds init-fns) deal-info u)
         triggers ((:triggers init-fns) deal-info u)
         expense ((:expense init-fns) deal-info u)
         base-snapshot-date (jt/local-date u)

         ;proj-start-index (get-in deal-info [:snapshot u :信息 :当前期数])
         ]
     (-> deal-info
         (assoc-in [:projection :dates] (util/filter-projection-dates dates base-snapshot-date)) ;need to truncate by update date
         (assoc-in [:update :info] {:update-date u})
         (assoc-in [:update :pool] pool)
         (assoc-in [:update :bond] bond)
         (assoc-in [:update :expense] expense)
         (assoc-in [:update :account] accounts)
         (assoc-in [:update :trigger] triggers)
         ;(assoc-in [:projection :period] (inc proj-start-index))
         (assoc-in [:waterfall] (zip/vector-zip (:waterfall deal-info)))
         )))

  ([deal-info]
   (let [avail-updates (:snapshot deal-info)
         latest-snapshot (-> (sort-by jt/local-date (keys avail-updates)) (last))]
     (load-deal deal-info latest-snapshot))
   )
  )
