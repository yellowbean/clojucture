(ns clojucture.util-table
  (:import [ tech.tablesaw.api Table Row]
             [clojucture Cashflow])
  )





(comment
(defn rows [ ^Cashflow this ]
    (let [ num-of-rows (.rowCount this)
        fr (Row. this)]
      (loop [ cr fr rl []]
        (if (.hasNext cr)
          (recur (.next cr) (cons cr rl))
          (cons cr rl))
      )
  )))

