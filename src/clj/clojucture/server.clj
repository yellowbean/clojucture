(ns clojucture.server
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.tools.cli :as cli]

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
(def config (atom {}))


(defn list-deals [ ws-id ]
  (let [
        f-list (->>
                 (seq (.list (io/file (:directory @config) ws-id)))
                 (filter #(.endsWith % ".edn")))
        ]
    {:workspace ws-id :models f-list}
    ))

(defn read-deal [ws-id deal-file-name]
  (let [deal-file-path (io/file (:directory @config) ws-id deal-file-name)]
    (cn/load-deal-from-file (.toString deal-file-path))))

(defn run-deal [ws-id deal-file-name assump]
  (let [deal-loaded (read-deal ws-id deal-file-name)]
    (:projection (cn/run-deal deal-loaded assump))))


(defroutes local-server
           (GET "/" [] "<h3>Alive!</h3>")
           (GET "/version" [] (-> {:version version} (json/write-str)))
           (GET "/ping" [] (-> {:version version :ip "localhost"} (json/write-str)))
           (context "/workspace/:ws-id" [ws-id]
             (GET "/" [] (-> {:workspace ws-id} (json/write-str)))
             (GET "/deals" [] (-> (list-deals ws-id) (json/write-str)))
             (context "/:d-name" [d-name]
               (GET "/" [] (-> (read-deal ws-id d-name) (json/write-str)))
               ;(GET "/run" [ assump ] (-> (read-deal ws-id d-name ) (cn/run-deal assump) (json/write-str)))
               )
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
  (let [cli-m (cli/parse-opts args cli-options) ]
    (reset! config (:options cli-m))
    (println @config)
    (start-server @config)
    ))