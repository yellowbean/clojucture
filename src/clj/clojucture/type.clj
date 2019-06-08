(ns clojucture.type )


(defprotocol Asset
	(project-cashflow [ x ] [ x assump ]  "project cashflow with/out assumption")
	)

(defprotocol Liability
  (cal-due-amount [ x d ] [ x d base ] )
  (receive [x d amount ] )
  )

(defprotocol IndexCurve
  (apply-to [ x float-info dates  ]))

(defprotocol pTable
	(merge1 [x t]))

(defprotocol Account
	(withdraw [ x d to amount ])
  (try-withdraw [ x d to amount ])
	(deposit [ x d from amount ])
  (last-txn [ x ])
	)

