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


(defn run-deal [ws-path deal-file-name assump]
  (let [deal-file-path (io/file ws-path deal-file-name)
        deal (cn/load-deal-from-file (.toString deal-file-path)) ]
    (:projection (cn/run-deal deal assump))
    ))


(defn deal-routes [  d-name]
  (routes
    (GET "/run" [ ]
         (-> (run-deal nil d-name nil)
             (json/write-str)) )
    )
  )


(defn ws-routes [ws-id]
  (routes
    (GET "/" [] (-> {:workspace ws-id } (json/write-str) )
    (GET "/deals" [] (list-deals ws-id))
    (PUT "/deals/:deal-file" [deal-file] "")

    (context "/deal/:deal-name" [deal-name]
             (deal-routes deal-name))
    )))

(defroutes local-server
           (GET "/" [] "<h3>Alive!</h3>")
           (context "/workspace/:ws-id" [ws-id]
                    (ws-routes ws-id)))

(def shandler (handler/site local-server))

(defn start-server [config]
  (->
    shandler
    (ring-jetty/run-jetty {:port (:port config) :auto-reload? true})
    ))
