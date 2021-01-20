(ns clojucture.assumption_test
  (:require [clojure.test :refer :all]
            [java-time :as jt]
            [clojucture.util :as u]
            [clojucture.assumption :as assump]
            [clojucture.assumption :as a])
  (:import [java.time LocalDate]
           [clojucture RateAssumption]
           ))



(deftest zAssumption
  (let [f (jt/local-date 2018 1 1)
        e (jt/local-date 2019 1 1)
        da (u/dates [f e])
        rate-ary (u/ldoubles [0.0])
        ra (RateAssumption. "t" da rate-ary )

        prj-dates (u/dates [(jt/local-date 2018 3 1) (jt/local-date 2018 6 1)])
        rproj (.project ra prj-dates )
        rapply (.apply ra prj-dates) ]

    ; RateAt
    (is (= (.rateAt ra (jt/local-date 2018 6 1)) 0.0))

    ; Project
    (is (=  (.get (.column rproj "Start") 0) (jt/local-date 2018 3 1)))
    (is (=  (.get (.column rproj "End") 0) (jt/local-date 2018 6 1) ))

    ; Apply
    (is (= (first rapply) 0.0))
    (is (= (count rapply) 1))
    ;(println rapply)


    ))

(deftest rAssumption
  (let [ da (jt/local-date 2018 1 1 )
         db (jt/local-date 2018 2 1 )
         dc (jt/local-date 2018 3 1 )

         d-array (u/dates [ da db dc])
         rate-ary (u/ldoubles [ 0.1  0.2 ])

         ra (RateAssumption. "t" d-array rate-ary)
         pj-dates (u/dates [  (jt/local-date 2018 1 15) (jt/local-date 2018 2 15) ])
         rproj (.project ra pj-dates)
         ;rapply (.apply ra pj-dates)
        ]
    ;(println rproj)
    (is (= (.get (.column rproj "Rate") 0) 0.1))
    (is (= (.get (.column rproj "Rate") 1) 0.2))


    ;(println rapply)

    ;(is (= (first rapply ) 1.60) )
    ;(is (= (second rapply ) 2.60) )
    )
  )

(deftest pAssumpiton
  (let [  dlist [ (jt/local-date 2018 1 1) (jt/local-date 2018 2 1 )]
        dassump (assump/gen-pool-assump-df :cpr [ 0.1 ] dlist)
        sassump (assump/gen-pool-assump-df :cdr 0.3 dlist)

        dr (assump/cpr2d 0.1)
        dr2 (assump/cpr2d 0.3)
        ]
    (is (= (.rateAt dassump (jt/local-date 2018 1 1)) dr))
    (is (= (.rateAt dassump (jt/local-date 2018 1 5)) dr))

    (is (= (.rateAt sassump (jt/local-date 2018 1 1)) dr2))
    (is (= (.rateAt sassump (jt/local-date 2018 1 5)) dr2))

    )
  )


(deftest genAssumptionCurve
  (let [  ods (u/gen-dates (jt/local-date 2018 1 1) (jt/months 1) 10)

         assump-date-range (u/dates [(jt/local-date 2018 1 1 ) (jt/local-date 2018 10 1)  ])
         r (u/ldoubles [0.12])

         assp {:prepayment (RateAssumption. "P" assump-date-range r )
                :default (RateAssumption. "P" assump-date-range  r ) }
        {ppy-curve :prepayment-curve def-curve :default-curve} (a/gen-assump-curve ods assp)
        ]
    (is (= (count ods) (inc (count ppy-curve))))
    (is (= (count ods) (inc (count def-curve))))
    )
  )

(comment

 (def test-curve (assump/setup-curve
                   :一年以内贷款利率
                   [(jt/local-date 2018 1 1) (jt/local-date 2018 6 1)]
                   [0.43 0.49]))
                  

 (deftest tSetupCurve
   (let [Y1M (assump/setup-curve :Libor1M
                                 (u/gen-dates (jt/local-date 2019 2 1) (jt/months 1) 5)
                                 [0.1 0.2 0.3 0.4 0.5])

         L1M_rates (:Libor1M Y1M)
         L1M_df (assump/curve-to-df "Libor1M" L1M_rates)]
     (is (= (second (first L1M_rates)) 0.1))
     (is (= 1 (count Y1M)))

     (is (= (second (last L1M_rates)) 0.5))

     (is (= 5 (.rowCount L1M_df)))
     (is (= 0.1 (.get (.column L1M_df "Double") 0)))
     (is (= 0.1 (.get (.column L1M_df "Double") 0)))))


    


 (deftest tApplyCurve
   (let [ curves (assump/setup-curve :Libor1M
                   (u/gen-dates (jt/local-date 2019 2 1) (jt/months 1) 5)
                   (range 0.01 0.06 0.01))
         reset-ds1 (u/gen-dates (jt/local-date 2019 2 1) (jt/months 1) 5)
         reset-ds2 (u/gen-dates (jt/local-date 2019 2 2) (jt/months 1) 5)
         reset-ds3 (u/gen-dates (jt/local-date 2019 1 29) (jt/months 1) 5)

         float-info1 {:index :Libor1M :reset reset-ds1 :margin 0.01}
         float-info2 {:index :Libor1M :reset reset-ds1 :factor 1.1}

         result-rate1 (assump/apply-curve curves float-info1)
         result-rate2 (assump/apply-curve curves float-info2)]
        ;result-rate3 (assump/apply-curve curves float-info1 reset-ds3)
        

    ;(is (= (first (:reset-dates result-rate1)) (jt/local-date 2019 2 1)))
    ;(is (= (last (:reset-dates result-rate1)) (jt/local-date 2019 6 1)))
     (is (= (first result-rate1) 0.02))
     (is (< (- (last result-rate1) 0.06  )  0.001))

     (is (< (- (first  result-rate2) 0.011  ) 0.001))
     (is (< (- (last result-rate2) 0.055  )  0.001))))

    
  
 (deftest tPoolAssump
   (let [ aSMM (assump/gen-pool-assump-df :smm [0.01 0.02] [(jt/local-date 2018 1 1) (jt/local-date 2018 3 1)])]
     (is (= (first (.column aSMM 0)) (jt/local-date 2018 1 1)))
     (is (= (last (.column aSMM 0)) (jt/local-date 2018 3 1)))

     (is (= (first (.column aSMM 1)) 0.0))
     (is (< (- (first (.column aSMM 1)) 0.019333333) 0.001)))
    
   (let [ aCPR (assump/gen-pool-assump-df :cpr [0.03 0.05] [(jt/local-date 2018 1 1) (jt/local-date 2018 3 1)])
         aCDR (assump/gen-pool-assump-df :cdr [0.03 0.05] [(jt/local-date 2018 1 1) (jt/local-date 2018 3 1)])]


     (is (< (- (first (.column aCPR 1)) 0.004849) 0.001))
     (is (< (- (first (.column aCDR 1)) 0.004849) 0.001)))))


(deftest tBuild
  (let [
        b1 (assump/build  {:p {:name :prepayment :type :cpr
                           :dates  [(jt/local-date 2018 1 1) (jt/local-date 2019 1 1) ]
                           :values [0.5] }
                          :d {:name :default :type :cdr
                           :dates  [(jt/local-date 2018 1 1) (jt/local-date 2019 1 1) ]
                           :values [0.5] }})
        ]
    (println b1)
    )
  (comment
    (let [
          b2 (assump/build {:prepayment
                            [[(jt/local-date 2018 1 1) 0.3] [(jt/local-date 2019 1 1) 0.5]]})
          ]
      )

    (let [
          b3 (assump/build {:prepayment
                            [[(jt/local-date 2018 1 1) 0.3] [(jt/local-date 2019 1 1) 0.5]]})
          ]
      )
    )
  )


(deftest tApplyPoolAssump
  (let [aCDR (assump/gen-pool-assump-df :cdr
                [0.03 0.04 0.045]
                [(jt/local-date 2018 1 1) (jt/local-date 2018 2 1) (jt/local-date 2018 3 1) (jt/local-date 2018 4 1)])
        od (into-array LocalDate [ (jt/local-date 2018 1 10) (jt/local-date 2018 2 10) (jt/local-date 2018 2 25)])
        projected-pool-assump (.project aCDR od)

        dr-1 (assump/cpr2d 0.03)
        dr-2 (assump/cpr2d 0.04)
        dr-3 (assump/cpr2d 0.045)
        ]
        
    (is (= (jt/local-date 2018 2 1) (.get projected-pool-assump 0 1)))
    (is (= (jt/local-date 2018 1 10) (.get projected-pool-assump 0 0)))
    (is (< (Math/abs (- dr-1 (.get projected-pool-assump  0 2))) 0.0001 ))
    (is (< (Math/abs (- dr-2 (.get projected-pool-assump  1 2))) 0.00001 ))
    (is (= (.rowCount projected-pool-assump) 4))

  ))
  

(deftest curveConvertion

  (is (< (- (assump/smm2cpr 0.03 ) 0.3061)  0.001))
  (is (< (- (assump/cpr2smm 0.15 ) 0.01345)  0.001)))

  

