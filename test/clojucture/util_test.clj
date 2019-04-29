(ns clojucture.util_test
  (:require
    [clojure.test :refer :all]
    [clojucture.util :as u]
    [java-time :as jt])
  )


(def test-curve [ [(jt/local-date 2018 1 1) 0.04] [(jt/local-date 2018 6 1) 0.05] ])
(def test-dates-ary (u/gen-dates-ary (jt/local-date 2018 1 1) (jt/months 1) 12 ))

(vec (u/gen-float-period-rates test-dates-ary test-curve))


; date generation
(deftest date-vector
  (let [ date-vector-1 (u/gen-dates (jt/local-date 2018 1 1) (jt/months 1) 20)
         date-vector-2 (u/gen-dates (jt/local-date 2018 1 1) (jt/years 1) 3)
         date-vector-3 (u/gen-dates (jt/local-date 2018 1 1) (jt/months 2) 5)
        ]
    (is (= (first date-vector-1) (jt/local-date 2018 1 1)))
    (is (= (last date-vector-1) (jt/local-date 2019 8 1)))
    (is (= (count date-vector-1) 20))

    (is (= (second date-vector-2) (jt/local-date 2019 1 1)))
    (is (= (second date-vector-3) (jt/local-date 2018 3 1)))

    )
  )

; date generation - range

(deftest date-vector2
  (let [ date-vector-1 (u/gen-dates-range (jt/local-date 2019 2 10) (jt/months 1) (jt/local-date 2020 3 20))
         date-vector-2 (u/gen-dates-range (jt/local-date 2019 2 10) (jt/months 1) (jt/local-date 2020 3 20) :month-end)

        ;quarterly
        date-vector-3 (u/gen-dates-range (jt/local-date 2019 2 10) (jt/months 3) (jt/local-date 2020 3 20) :month-end)

        ;semi-annually
        ]
    (is (= (first date-vector-1) (jt/local-date 2019 2 10)))
    (is (= (second date-vector-1) (jt/local-date 2019 3 10)))
    (is (= (last date-vector-1) (jt/local-date 2020 3 10)))

    (is (= (first date-vector-2) (jt/local-date 2019 2 28)))
    (is (= (second date-vector-2) (jt/local-date 2019 3 31)))
    (is (= (last date-vector-2) (jt/local-date 2020 3 31)))

    (is (= (first date-vector-3) (jt/local-date 2019 2 28)))
    (is (= (second date-vector-3) (jt/local-date 2019 5 31)))
    (is (= (last date-vector-3) (jt/local-date 2020 2 29)))
    )
  )


(deftest gen-column-test
  (let [ dat-col (u/gen-column [:date-col (u/gen-dates-ary (jt/local-date 2018 1 1) (jt/months 1) 4)])
         dou-col (u/gen-column [:double-col (double-array [1 2 3 4])])
         ;str-col (u/gen-column [:string-col (into-array String ["HEE" "LOO" "WWW" "WORD"])])
        ]
    (is (= (.size dat-col) 4))
    (is (= (.size dou-col) 4))
    ;(is (= (.size str-col) 4))
  )
  )


(deftest working-day
  (let [ bench-date (jt/local-date 2019 2 5)
        p4-date (u/previous-n-workday bench-date 4)
        n4-date (u/next-n-workday bench-date 4)
        ]
    (is (= p4-date  (jt/local-date 2019 1 30)))
    (is (= n4-date  (jt/local-date 2019 2 11))) ) )



; find first
(deftest find-first-test
  (let [test-d (u/gen-dates (jt/local-date 2018 1 1) (jt/months 1) 20)
        found-d-1 (u/find-first-date (jt/local-date 2018 4 2) test-d :before)
        found-d-2 (u/find-first-date (jt/local-date 2018 4 2) test-d :after)]
    (is (= found-d-1 (jt/local-date 2018 4 1) ))
    (is (= found-d-2 (jt/local-date 2018 5 1) ))
    ))

; find first in vector filled with maps
(deftest t-find-first-in-vec
  (let [ test-vm [{:dates (jt/local-date 2018 1 1) :balance 80}
                  {:dates (jt/local-date 2018 2 3) :balance 100}
                  {:dates (jt/local-date 2018 5 1) :balance 150}
                  {:dates (jt/local-date 2018 8 1) :balance 200}]
        ]
    (is (=
          (:dates (u/find-first-in-vec (jt/local-date 2018 4 3) test-vm :dates jt/before? :before))
          (jt/local-date 2018 2 3)
          ))
    (is (=
          (:dates (u/find-first-in-vec (jt/local-date 2018 4 3) test-vm :dates jt/before? :after))
          (jt/local-date 2018 5 1)
          ))
    (is (=
          (:dates (u/find-first-in-vec (jt/local-date 2018 5 1) test-vm :dates = :after))
          (jt/local-date 2018 5 1)
          ))
    )
  )


; init-column test
(deftest t-init-col
  (let [dc (u/init-column :double :DoubleColumn)
        bc (u/init-column :boolean :BoolColumn)]

    (is (= "DoubleColumn" (.name dc)))
    (is (= "BoolColumn" (.name bc)))
    )
  )

; init-table test
(deftest t-init-table
  (let [t (u/init-table "tInit"
            [ [:double :balance] [:boolean :triggered?] [:date :dates] [:string :flowname]])
        ]
    (is (= 4 (.columnCount t)))
    (is (= "balance" (.name (.column t 0))))
    (is (= "triggered?" (.name (.column t 1))))
    (is (= "dates" (.name (.column t 2))))
    (is (= "flowname" (.name (.column t 3))))

    )
  )
