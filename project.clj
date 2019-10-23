(defproject clojucture "0.0.4-SNAPSHOT"
  :description "a clojure library for modelling & analysis structure products (CLO/MBS/ABS)"
  :url "https://yellowbean.github.io/clojucture/"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/clojure "1.10.1"]
                 [tech.tablesaw/tablesaw-core "0.32.7"]
                 [org.apache.commons/commons-math3 "3.6.1"]
                 [org.apache.commons/commons-lang3 "3.7"]
                 ;; https://mvnrepository.com/artifact/org.apache.commons/commons-collections4
                 ;[org.apache.commons/commons-collections4 "4.4"]


                 [org.threeten/threeten-extra "1.4"]
                 [nrepl "0.6.0"]
                 [clojure.java-time "0.3.2"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.zip "0.1.2"]
                 [org.clojure/core.match "0.3.0"]
                 [dk.ative/docjure "1.12.0"]
                 [org.clojure/tools.cli "0.4.2"]
                 [medley "1.2.0"]
                 [com.rpl/specter "1.1.3"]

                 [ring "1.7.1"]
                 [compojure "1.6.1"]

                 ]
  :plugins [
            ;[lein-virgil "0.1.9"]
            ]
  :main clojucture.server
  :mirrors {"central" {:name "aliyun maven"
                       :url  "https://maven.aliyun.com/nexus/content/groups/public/"}}
  ;:user {:plugins [ [nightlight/lein-nightlight "RELEASE"]]}
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :profiles {
             :uberjar {:aot :all}
             :dev     {
                       :plugins [
                                 [lein-ring "0.12.5"]
                                 [lein-shell "0.5.0"]
                                 [lein-virgil "0.1.9"]
                                 [jonase/eastwood "0.3.5"]
                                 ]
                       }
             }
  :aliases
  {"native"
   ["shell"
    "native-image" "--report-unsupported-elements-at-runtime"
    "--initialize-at-build-time"
    "-jar" "./target/${:uberjar-name:-${:name}-${:version}-standalone.jar}"
    "-H:Name=./target/${:name}"]}
  :ring {:handler clojucture.server/shandler}
  )
