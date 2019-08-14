(ns clojucture.server
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]

    [compojure.core :refer :all]
    [compojure.route :as route]
    [compojure.handler :as handler]
    [ring.adapter.jetty :as ring-jetty]

    [java-time :as jt]

    [clojucture.local.china.local_cn :as cn]
    [clojucture.util :as u]
    )
  (:import (java.nio.file Paths))
  )


(def config {:port 8080
             :root "C:\\changer\\engine\\resources"

             })



(defn list-deals [wk-path]
  (let [f-list (->>
                 (seq (.list (io/file (:root config) wk-path)))
                 (filter #(.endsWith % ".edn")))
        r {:workspace (keyword wk-path) :models f-list}
        ]
    (json/write-str r)
    ))


(defn run-deal [ ws-id deal-file-name assump]
  (let [ deal-loaded (read-deal ws-id deal-file-name) ]
    (:projection (cn/run-deal deal-loaded assump)) ))


(defn read-deal [ws-id deal-file-name]
  (let [ deal-file-path (io/file (:root config) ws-id deal-file-name) ]
    (cn/load-deal-from-file (.toString deal-file-path)) ) )

(defroutes local-server
           (GET "/" [] "<h3>Alive!</h3>")
           (GET "/TEST" [] "<h3>Test!</h3>")
           (context "/workspace/:ws-id" [ws-id]
                    (GET "/" [] (-> {:workspace ws-id } (json/write-str) ))
                    (GET "/deals" [] (list-deals ws-id))
                    (context "/:d-name" [ d-name ]
                             (GET "/" [] (-> (read-deal ws-id d-name ) (json/write-str)))
                             (GET "/run" [ assump ] (-> (read-deal ws-id d-name ) (json/write-str)))

                             )
                    )
           )

(def shandler (handler/site local-server))

(defn start-server [config]
  (->
    shandler
    (ring-jetty/run-jetty {:port (:port config) :auto-reload? true})
    ))
