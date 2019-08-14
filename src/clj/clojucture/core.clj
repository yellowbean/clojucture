(ns clojucture.core
  (:require
    [clojure.core.match :as m]
    [clojure.java.io :as io]
    [clojure.tools.cli :refer [parse-opts]]
    )
  (:import
    [java.time LocalDate])
  (:gen-class)
  )

(def engine-version 0.1)


(def cli-options
  [
   ["-p" "--port PORT" "Port number"
      :default 15013
      :parse-fn #(Integer/parseInt %)
      :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]

   ["-c" "--config CONFIG" "Config file path"]
      :default nil
      :validate [ #(.exists (io/as-file %))  "Config file doesn't exists"]

   ["-h" "--help"]
  ])



(defn -main [ & args]
  (let [ cli-m (parse-opts args cli-options)]

  ))

