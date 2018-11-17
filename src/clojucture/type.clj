(ns clojucture.type
  (:require [clojucture.util :as u])
  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn StringColumn BooleanColumn]
    [tech.tablesaw.columns AbstractColumn]
    ;(clojucture.type Cashflow)

    ;(clojucture.type IndexCurve)
    )

	)




(defprotocol Asset
	(project-cashflow [ x ] [ x assump ]  "project cashflow with/out assumption")
	)

(defprotocol Liability
  (cal-due-amount [ x d ] )
  (receive [x d amount])
  )



(comment
(defprotocol Pool
	(project-cashflow [ x ] )
	(collect-cashflow [ x ] )
	)
)

(defprotocol Bond
  (cal-due-principal [ x ] )
  (cal-due-interest [ x ] )
	;(load [ s ])
	)

(defprotocol Deal
	(run-assets [x assump] )
	(run-bonds [x assump] )
	)


(defprotocol IndexCurve
  (apply-to [ x float-info dates  ]))

(defprotocol pTable
	(merge1 [x t]))

(defprotocol Account
	(withdraw [ x d to amount ])
	(deposit [ x d from amount ])
	)


(defn init-statement [ x ]
  ;(u/gen-table "statement"
  ;             {:dates dates })
  )

;(extend Table
;  pTable
;  {:merge u/merge-table }
;  )