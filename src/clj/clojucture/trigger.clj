(ns clojucture.trigger
  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn]
    )
  )

(defprotocol Trigger
  (breach? [ x d current-level ])
  )

(defrecord trigger [ info threshold status ]
  Trigger
  (breach? [ x d current-level ]

    )
  )


