(ns clojucture.reader.base
  (:require
    [java-time :as jt]
    [clojure.core.match :as m]
    [clojucture.util :as u])
  (:use clojure.core.match.regex)
  (:import [ java.time LocalDate])


  )

(defn parsing-dates [x]

    (m/match x
             #"\d{4}\-\d{2}\-\d{2}Q,\d{4}\-\d{2}\-\d{2}"
             (as-> (re-matches #"(\S+)Q,(\S+)" x ) [ _ s ed]
                   (concat
                     (u/gen-dates-range (jt/local-date s) (jt/months 3) (jt/local-date ed))
                     [(jt/local-date ed)])
                   )

             #"(\S+),(\S+)"
             (as-> (re-matches #"(\S+),(\S+)" x) [_ first-match last-match]
                   (flatten (list (parsing-dates first-match) (parsing-dates last-match))))
             #"\d{4}\-\d{2}\-\d{2}"
             (jt/local-date x)
             #"\d{4}\-\d{2}\-\d{2}M,\d{4}\-\d{2}\-\d{2}" ;first date as s, following month at day d
             (as-> (re-matches #"(\S+)M,(\S+)"  x) [ _ s ed]
                   (concat
                     (u/gen-dates-range (jt/local-date s) (jt/months 1) (jt/local-date ed))
                     [(jt/local-date ed)]))
           #"\d{4}\-\d{2}\-\d{2}ME,\d{4}-\d{2}\-\d{2}"
           (as-> (re-matches #"(\S+)ME,(\S+)" ,x ) [ _ s ed]
                         (cons (jt/local-date s)
                       (u/gen-period-end-dates
                         (.plusMonths (jt/local-date s) 1)
                         (jt/months 1)
                         (jt/local-date ed))))

           #"\d{4}\-\d{2}\-\d{2}\w{1,2}[+-]\dWD,\d{4}\-\d{2}-\d{2}"
              (as-> (re-matches #"(\d{4}\d)WD,(\S+)" x) [ _ s adj days ed ]
                 (let [ bech-dates (parsing-dates s  )
                       adj-days (Integer/parseInt days)
                       adj-dates (case adj
                                     "+" (map #(u/next-n-workday % adj-days) bech-dates)
                                   "-" (map #(u/previous-n-workday % adj-days) bech-dates)
                                   ) ]
                   adj-dates))

           :else nil )
  )