(ns clojucture.account
  (:require [clojucture.type :as t]
            [java-time :as jt]
            )
  (:import [java.time LocalDate])
  )


(defrecord stmt [ ^LocalDate date from to amount info ]

  )

(defrecord account [ name type balance stmts ]
  t/Account
  (withdraw [ x  d to  amount ]
    (.deposit x (- d) to amount)
    )
  (deposit [ x  d from   amount ]
    (let [ new-statment (->stmt d from :this amount nil)
           new-statments (cons stmts new-statment)
           new-balance (+ balance amount)
          ]
      (->account name type new-balance new-statments)
      )
    )
  )

(defn transfer-fund [ from to amount ]

  )