(ns clojucture.type )




(defprotocol Liability
  (cal-due-amount [ x d ] [ x d base ] )
  (receive [x d amount ] )
  )

(defprotocol IndexCurve
  (apply-to [ x float-info dates  ]))

(defprotocol pTable
	(merge1 [x t]))



