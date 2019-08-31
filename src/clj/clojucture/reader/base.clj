(ns clojucture.reader.base
  (:require
    [java-time :as jt]
    [clojure.core.match :as m]
    [clojucture.util :as u]
    [clojure.string :as str])
  (:use clojure.core.match.regex))

(defn parsing-dates [x]
  "given input string ,return a date or a vector of dates"
  (m/match x

           #"\d{4}\-\d{2}\-\d{2}M,\d{4}\-\d{2}\-\d{2}"      ;first date as s, following month at day d
           (as-> (re-matches #"(\S+)M,(\S+)" x) [_ s ed]
                  (u/gen-dates-range
                         (jt/local-date s)
                         (jt/months 1)
                         (jt/local-date ed))   )


           #"\d{4}\-\d{2}\-\d{2}ME,\d{4}-\d{2}\-\d{2}"
           (as-> (re-matches #"(\S+)ME,(\S+)", x) [_ s ed]
                   (u/gen-period-end-dates
                   (jt/local-date s)
                   (jt/months 1)
                   (jt/local-date ed))
                   )

           #"^\d{4}\-\d{2}\-\d{2}Q,\d{4}\-\d{2}\-\d{2}$"
           (as-> (re-matches #"^(\d{4}\-\d{2}\-\d{2})Q,(\d{4}\-\d{2}\-\d{2})$" x) [_ s ed]
                   (u/gen-dates-range
                     (jt/local-date s)
                     (jt/months 3)
                     (jt/local-date ed))
                   )

           #"^\d{4}\-\d{2}\-\d{2},\d{4}\-\d{2}\-\d{2}$"
           (as-> (re-matches #"(\d{4}\-\d{2}\-\d{2}),(\d{4}\-\d{2}\-\d{2})" x) [_ f l]
                 (list (parsing-dates f) (parsing-dates l))
                 )

           #"\d{4}\-\d{2}\-\d{2}\w{1,2}[+-]\dWD,\d{4}\-\d{2}-\d{2}"
           (as-> (re-matches #"(\S+)([+-])(\d+)WD,(\S+)" x) [_ s adj days ed]
                 (let [bech-dates (parsing-dates (str s "," ed))
                       adj-days (Integer/parseInt days)     ;business day
                       adj-dates (case adj
                                   "+" (map #(u/next-n-workday % adj-days) bech-dates)
                                   "-" (map #(u/previous-n-workday % adj-days) bech-dates)
                                   )]
                   adj-dates))

           #"(\S+?),(\S+)"
           (as-> (re-matches #"(\S+?),(\S+)" x) [_ first-match last-match]
                 (flatten (list (parsing-dates first-match) (parsing-dates last-match))))


           #"(\S+),(\S+),?"
           (as-> (re-matches #"(\S+),(\S+)" x) [_ first-match last-match]
                 (flatten (list (parsing-dates first-match) (parsing-dates last-match))))

           #"\d{4}\-\d{2}\-\d{2}"
           (jt/local-date x)




           :else (println "failed with " x))
  )

(defn parsing-doubles [ x ]
  "input: a string with doubles seperated by comma;
  output: a vector with doubles"
  (->> (str/split x #",")
      (map #(Double/parseDouble %) )
      )
  )