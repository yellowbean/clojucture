(ns clojucture.server
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.tools.cli :as cli]
    [clojure.core.match :as m]

    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler :as handler]
    [ring.adapter.jetty :as ring-jetty]

    [java-time :as jt]

    [clojucture.local.china.local_cn :as cn]
    [clojucture.spv :as spv]
    [clojucture.reader.base :as base]
    [clojucture.assumption :as assump]
    [clojucture.util :as u]
    [clojure.string :as str])
  (:import (java.nio.file Paths))
  (:gen-class)
  )

(def version 0.03)
(def config (atom {:port 3001 :directory "C:\\changer\\engine\\resources"}))
;(def config (atom {}))


(defn list-deals [ws-id]
  "list all deals(files end with `edn`) in workspace"
  (let [all-file-list (seq (.list (io/file (:directory @config) ws-id)))]
    {:workspace ws-id :models (filter #(.endsWith % ".edn") all-file-list)}
    ))

(defn read-deal [ws-id deal-file-name]
  "read deal file and return in a map"
  (let [deal-file-path (io/file (:directory @config) ws-id deal-file-name)]
    (spv/load-deal-from-file (.toString deal-file-path))))


(defn -parse-assumption [ x ]
  "parse an assumption string into an assumption map"
  (let [  assump-tokens (str/split x #":") ]
  (m/match assump-tokens
           [ assump-type  v-type ds vs]
           {:name (keyword assump-type) :type (keyword v-type) :dates (base/parsing-dates ds) :values (base/parsing-doubles vs) }


           :else :not-match-assump-token
           )
  )
  )


(defn read-assumption [ x ]
  "convert assumption from POST form into clojure map"
  (let [ assump-list (str/split x #";")
         assump-inst-list (map -parse-assumption assump-list) ]

    ;(println assump-inst-list)
    (assump/build assump-inst-list)

    ) )


(defn run-deal [ws-id deal-file-name assump ]
  (let [deal-loaded (read-deal ws-id deal-file-name)
       assump-to-run (read-assumption assump) ]
    ;(println "ASTR" assump-to-run)
    (:projection (spv/run-deal deal-loaded assump-to-run))

    ))

(defroutes local-server
           (GET "/" [] (-> {:message "Welcome !" :version version} (json/write-str)))
           (GET "/ping" [] (-> {:version version :ip "localhost"} (json/write-str)))
           (GET "/workspaces" [] (-> {:workspaces ["A" "B"]} (json/write-str)))
           (context "/workspace/:ws-id" [ws-id]
             (GET "/" [] (-> {:workspace ws-id} (json/write-str)))
             (GET "/deals" [] (-> (list-deals ws-id) (json/write-str)))
             (GET "/deal/:d-name" [d-name] (-> (read-deal ws-id d-name) (json/write-str)))
             (POST "/deal/:d-name/analysis" [ d-name & x ]
               (-> (run-deal ws-id d-name (:assumption x))
                   (json/write-str)))
             ;(GET "/run" [ assump ] (-> (read-deal ws-id d-name ) (cn/run-deal assump) (json/write-str)))
             )
           )

(def shandler (handler/site local-server))


(defn start-server [config]
  (->
    shandler
    (ring-jetty/run-jetty {:port (:port config)})))

(def cli-options
  [
   ["-p" "--port PORT" "Port number"
    :default 15013
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]


   ["-d" "--directory DIRECTORY" "working directory"]
   ;["-c" "--config CONFIG" "Config file path"
   ;:default nil
   ;:parse-fn #(-> % (slurp) (json/read-str))
   ;:validate [#(.exists (io/as-file %)) "Config file doesn't exists"]]

   ["-h" "--help"]
   ]
  )

(defn -main [& args]
  (let [cli-m (cli/parse-opts args cli-options)]
    (reset! config (:options cli-m))
    (start-server @config)
    ))