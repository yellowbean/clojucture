(ns clojucture.reader.cn
  (:require
    [clojucture.asset :as asset]
    [clojucture.expense :as exp ]
    [clojucture.util :as u]
    [clojucture.bond :as b]
    [clojure.java.io :as io]
    [clojure.core.match :as m]
    )



  (:use [ dk.ative.docjure.spreadsheet ])
  (:import
    [tech.tablesaw.api Table Row DoubleColumn DateColumn StringColumn BooleanColumn]
    ;[tech.tablesaw.io DataFrameReader]
    [java.util Locale]
    [tech.tablesaw.io.csv CsvReader CsvReadOptions$Builder]
    )
  )


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
           (exp/->recur-expense {:name n :amount a :period i :start-date s :end-date end-date } [] 0)
     {:name n :amount a}
           (exp/->amount-expense {:name n} [] nil a )
     {:name n :rate r :base b}
           (exp/->pct-expense-by-amount {:name n :base b :pct r} [] nil 0 )
     :else nil
    )
  )

(defn cn-setup-wf [ dist-action]
  (m/match dist-action



     )
  )

(defn cn-setup-pool [ a ]
  (m/match a
   {}



   )
  )

(defn cn-load-model [ wb-path ]
  (let [ wb (load-workbook wb-path)
        info-s (select-sheet "info" wb)
        [ closing-date settle-date first-payment-date stated-maturity ]
          (map #(:date %) (select-columns {:A :name :B :date} info-s))
        [ h & assets ] (row-seq (select-sheet "pool" wb))

        pool-s (select-sheet "pool" wb)
        pool (map cn-setup-pool
                  (subvec (select-columns
                            {:B :originate-date :C :maturity-date :D :closing-date :F :current-balance :G :rate :H :int-feq :I :int-interval} pool-s) 2))

        bond-s (select-sheet "bond" wb)
        bonds (map #(cn-setup-bond % settle-date )
                   (rest (select-columns {:A :name :B :balance :C :rate :D :feq  :E :type } bond-s)))

        fee-s (select-sheet "fee" wb)
        fees-oneoff (map #(cn-setup-fee % closing-date) (subvec (select-columns {:A :name :B :amount} fee-s) 2 ))
        fees-base (map #(cn-setup-fee % closing-date) (subvec (select-columns {:D :name :E :rate :F :base} fee-s) 2 ))
        fees-recur (map #(cn-setup-fee % closing-date) (subvec (select-columns {:H :name :I :amount :J :start-date :K :interval } fee-s) 2 ))

        waterfall-s (select-sheet "waterfall" wb)
        wf-norm (map cn-setup-wf
                     (subvec (select-columns {:A :cons :B :source :C :target :D :opt } waterfall-s) 2 ))

        wf-default (map cn-setup-wf
                     (subvec (select-columns {:F :cons :G :source :H :target :I :opt } waterfall-s) 2 ))

        
        assump-s (select-sheet "assumption" wb)
        result-s (select-sheet "result" wb)
        ]
    )
  )



(comment
(defn load-asset [ fp opt ]

  )


(defn reader-options [ opts ]
  (let [ b (CsvReadOptions$Builder.)]
    (cond-> b
      (contains? opts :locale ) (.locale (:locale opts))
      (contains? opts :header ) (.header (:header opts))
      (contains? opts :file ) (.file (io/as-file (:file opts)))
            true (.build ))
  )
)

(defn read-into-table-csv [ reader-opts ]
  (let [ ro (reader-options reader-opts)
        t (Table/read)]
    (.csv t ro)
    )
  )



(defn field-type-map [ ^Row r k v]
  (case k
    :start-date (.getDate r v)
    :periodicity (-> (.getString r v) ((u/constant :periodicity)))
    :term (.getInt r v )
    :balance (.getDouble r v )
    :fee-rate (.getDouble r v)
    )
  )


(defn row-to-asset [ ^Row row field-map asset-type]
  (doseq [ [ k v ] field-map]
    (println (field-type-map row k v))
    )
  )

(defn table-to-asset [ table field-map asset-type ]
  (loop [ trs (.iterator table) r [] ]
    (if-not (.hasNext trs)
      r
      (let [ current-row (.next trs)]
        (recur trs (conj r (row-to-asset current-row field-map asset-type )))
      )
    )
  ))
;(defn load-xl [ fp sh-name column-flags asset-class opt ]
;  (let [ raw-maps (->> (load-workbook fp )
;              (select-sheet sh-name)
;              (select-columns column-flags))
;         remove-first-line (fn [x]     (if (get opt :header false)
;                                         (next x)
;                                         x
;                                         ))
;         asset-of-function (fn [ a ] (case a
;                                      :installments asset/map->installments
;                                      :mortgage asset/map->mortgage
;;                                      :loan asset/map->loan
;                                      ))
;        ]
;
;    (-> raw-maps
;        (remove-first-line) ((asset-of-function asset-class)))
;    ))

)