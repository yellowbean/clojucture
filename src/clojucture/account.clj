(ns clojucture.account
  (:require [clojucture.type :as t]
            [java-time :as jt]
            )
  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn StringColumn BooleanColumn]
    [java.time LocalDate])
  )


(defrecord stmt [ ^LocalDate date from to ^Double amount info ]

  )

(defrecord account [ name type ^Double balance stmts ]
  t/Account
  (withdraw [ x d to amount ]
    (.deposit x d to (- amount))
    )
  (deposit [ x  d from   amount ]
    (let [ new-statment (->stmt d from :this amount nil)
           new-balance (+ balance amount)]
      (->account name type new-balance (conj stmts new-statment))
      )
    )
  )

(defn transfer-fund [ from-acc to-acc ^LocalDate d ^Double amount ]
  (if (>= (:balance from-acc) amount)
    (dosync
      [ (.withdraw from-acc d to-acc amount) (.deposit to-acc d from-acc amount)]
      )
    nil)
  )

(defn pay-principal [ source target ^Double amount  ]

  )

(defn pay-interest [ source target ^Double amount ]

  )

(defn pay-expense [ source target ^Double amount ]

  )

(defn pay-tax [ source target ^Double amount ]

  )