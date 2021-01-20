(ns clojucture.io.html
  (:require [hiccup.core :as hc]
            [clojucture.util :as u]
            [com.rpl.specter :as s]
            [clojucture.spv :as spv])
  (:import [tech.tablesaw.api Table]
    ; [ java.io FileOutputStream ]
           (java.io File))
  )

(defn build-table-body [^Table x]
  (let [cols-with-strings (map #(u/cstr %) (.columns x))
        rows-with-strings (apply (partial map vector) cols-with-strings)]
    (for [row rows-with-strings]
      [:tr
       (for [e row] [:td e])])
    ))


(defn df-to-html [^Table df]
  (when df
    (let [title (.name df)
          col-names (.columnNames df)]
      (hc/html [:div
                [:h3 title]
                [:table
                 [:tr (for [cn col-names] [:th cn])]
                 (build-table-body df)]])))
  )

(defn dump-to-file [^Table x f]
  "dump a single dataframe to file"
  (spit f (df-to-html x)))

(defn deal-to-html [d f]
  (let [{acc-cf-list :account bnd-cf-list :bond pool-cf :pool-cf} (:cashflow (spv/query-projection d))
        {acc-stmts-list :account bnd-df-list :bond} (:statements (spv/query-projection d))
        ]
    (->>
      (hc/html [:body
                [:div#cashflows
                 [:div#pool-performance
                  (df-to-html pool-cf)
                  ]

                 [:div#account
                  [:table
                   ; [:tr (for [a-df acc-stmts-list] [:th (.name a-df)] )]
                   [:tr {:style "vertical-align: top"}
                    (for [a-df acc-stmts-list]
                      [:td (df-to-html a-df)])]]]

                 [:div#bond
                  [:table
                   ;[:tr (for [b-df bnd-df-list] [:th (.name b-df)])]
                   [:tr {:style "vertical-align: top"}
                    (for [b-df bnd-df-list]
                      [:td (df-to-html b-df)])]
                   ]
                  ]
                 ]
                ])
      (spit f)
      )
    )
  )

