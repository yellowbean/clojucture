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
    [clojucture.util :as u]
    )
  (:import (java.nio.file Paths))
  (:gen-class)
  )

(def version 0.03)
;(def config (atom {:port 3001 :directory "C:\\changer\\engine\\resources"}))
(def config (atom {}))


(defn list-deals [ws-id]
  "list all deals(files end with `edn`) in workspace"
  (let [all-file-list (seq (.list (io/file (:directory @config) ws-id))) ]
    {:workspace ws-id :models (filter #(.endsWith % ".edn") all-file-list )}
    ))

(defn read-deal [ws-id deal-file-name flavor]
  "read deal file and return in a map"
  (let [deal-file-path (io/file (:directory @config) ws-id deal-file-name)]
    (case flavor
      "china" (cn/load-deal-from-file (.toString deal-file-path))


      {:error (str "No flavor found for " flavor)}
      )
    ))

(comment
  (defn run-deal [ws-id deal-file-name assump]
    (let [deal-loaded (read-deal ws-id deal-file-name)]
      (:projection (cn/run-deal deal-loaded assump))))
  )

(defroutes local-server
           (GET "/" [] "<h3>Alive!</h3>")
           (GET "/version" [] (-> {:version version} (json/write-str)))
           (GET "/ping" [] (-> {:version version :ip "localhost"} (json/write-str)))
           (GET "/workspaces" [] (-> {:workspaces ["A" "B"]} (json/write-str)))
           (context "/workspace/:ws-id" [ws-id]
             (GET "/" [] (-> {:workspace ws-id} (json/write-str)))
             (GET "/deals" [] (-> (list-deals ws-id) (json/write-str)))
             (GET "/deal/:d-name" [ d-name flavor] (-> (read-deal ws-id d-name flavor) (json/write-str)))
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