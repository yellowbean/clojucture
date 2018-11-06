(comment
(ns clojucture.CN-reader
  (:require
    [clojure.java.io :as io]
    [clojure.data.csv :as csv]
    [clojucture.asset :as asset]
    [clojucture.deal :as d]
    [clojucture.bond :as b]
    [clojucture.pool :as p]
    [clojucture.util :as u]
    [java-time :as jt]
    [clojure.xml :as xml]
    [clojure.zip :as zip]
    [clojure.data.zip.xml :as zip-xml]
    [clojucture.asset :as a])
  (:import
    [tech.tablesaw.api Table ColumnType]
    ;[org.jsoup.nodes Document Element ]
    ;[org.jsoup.select Elements]
    ;[org.jsoup Jsoup]
    ;[tech.tablesaw.io.csv CsvReadOptions.Builder]
    )
  )


(defn parse-dates [ tg ]

  )

(defn parse-period [ tg ]
  (let [ attrs (:attrs tg)
         s-date (-> (:start attrs) (jt/local-date))
         e-date (-> (:end attrs) (jt/local-date))]
    (if-let [ interval (attrs :every)]
      (case interval
        "Y" (u/gen-dates-range s-date (jt/years 1) e-date)
        "M[-1]" (-> (u/gen-month-ends-dates s-date e-date) (u/gen-dates-intervals))
        )
      (list s-date e-date)
        )))

(defn parse-intervals [ tg  ]

  )

(defn find-by-tag [m tg-value]
    (let [tg-list (tree-seq #(vector? (:content %)) :content m)
          r-list  (filter #(= (:tag %) tg-value) tg-list)
          ]
      r-list
      )
  )

(defn find-by-tag2
  ([m tg-value parse-type]
   (if-let [ element (-> (find-by-tag2 m tg-value) :content)]
     (case parse-type
       :double (-> element first (Float.))
       :int (-> element first (Integer.))
         :date (-> element first (jt/local-date))
        :text (-> element first)
       :key (-> element first keyword)
       )
     nil
     ))
  ([m tg-value]
   (first (find-by-tag m tg-value))
    )

  )

(defn setup-chinese-account [ m ]
  (d/->account (get-in m [:attrs :name]) nil (Float. (get-in m [:attrs :init])) nil )
  )

(defn setup-chinese-bond [ m ]
  (let [
         b-type (get-in m [:attrs :type])
         bal (find-by-tag2 m :当前余额 :double)
         current-r (find-by-tag2 m :当前利率 :double)
         s-date (find-by-tag2 m :起息日 :date)
         per (find-by-tag2 m :付息频率 :text)
        ]
    ;[  b-type bal current-r s-date per]
    (case b-type
      "seq" (b/->sequence-bond nil bal current-r s-date per nil nil )

      )
    )
  )

(defmulti create-asset :tag )

(defmethod create-asset :按揭贷款
  [ a ]
  (let [ m   {:current-balance  (find-by-tag2 a :剩余面额 :double)
              :remain-term (find-by-tag2 a :剩余期数 :int)
              :start-date (find-by-tag2 a :起始日 :date)
              :periodicity ((:periodicity -mapper) (get-in (find-by-tag2 a :贷款期数) [:attrs :unit] ))
              :term   (find-by-tag2 a :初始期数 :int)
              :int_type (get-in (find-by-tag2 a :利息信息) [:attrs :type])
              :current-rate (find-by-tag2 a :当前利率 :double)
              :day-count (find-by-tag2 a :计息日历 :key)
              :float-info       {:reset-dates (-> (find-by-tag2 a :重置日) (parse-period))
                                 :index (find-by-tag2 a :基准利率 :key)
                                 :margin (find-by-tag2 a :利差 :double)
                                 }
              :opt {}
              }
        ]

    (asset/map->float-mortgage m)
    ))

(defn setup-chinese-asset [ a ]
  (create-asset a)
  )

(defn setup-chinese-pool [ m coll-intervals ]
  (let [ assets (map #(setup-chinese-asset %) (:content m))
        ]
    assets;assets
    ;(p/->pool assets coll-intervals)
    )
  )

(defn setup-collection-intervals [ m ]
  (let [] )
  )

(defn read-chinese-deal [ ^String p ]
  (let [ d (-> p io/as-file xml/parse )
         deal-infos (:content (find-by-tag2 d :基本信息 ))
         deal-start-date  (find-by-tag2 d :初始起算日 :date)
         deal-stated-maturity-date  (find-by-tag2 d :法定到期日 :date)
         pool-collect-intervals (map #(parse-period %) (:content (find-by-tag2 d :收款期间 )))
         acc-infos (:content (find-by-tag2 d :账户信息 ))
         accs-deal (map setup-chinese-account acc-infos)
         bond-infos (:content (find-by-tag2 d :债券信息 ))
         bonds-deal (map setup-chinese-bond bond-infos)
         pool-info-list  (find-by-tag d :资产池 )
         pool-deal (map #(setup-chinese-pool % pool-collect-intervals) pool-info-list)
        ]
    {:deal {:deal-start-date deal-start-date
            :deal-stated-maturity-date deal-stated-maturity-date
            ;:pool-collect-intervals pool-collect-intervals

            }
     :accounts accs-deal
     :bonds bonds-deal
     :pool pool-deal
     }
    ))






(defn option-csv-col-types [ x col-types ]
  (.columnTypes x (into-array ColumnType col-types)))

(defn peek-csv [ path ]
  (let [ file (io/file path)
         csv-filename (.getPath file) ]))

(defn read-csv [ path param ]
  (let [file (io/file path)
        csv-filename (.getPath file)
        source-table (doto (Table/read ) (.csv  (io/input-stream csv-filename) csv-filename ) )]
    )
)

)