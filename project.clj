(defproject clojucture "0.0.3-SNAPSHOT"
  :description "a clojure library for modelling & analysis structure products (CLO/MBS/ABS)"
  :url "https://yellowbean.github.io/clojucture/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [org.clojure/clojure "1.10.0"]
    [tech.tablesaw/tablesaw-core "0.32.7"]
    [org.apache.commons/commons-math3 "3.6.1"]
    [org.apache.commons/commons-lang3 "3.7"]
    [org.threeten/threeten-extra "1.4"]
    [nrepl "0.6.0"]
    [clojure.java-time "0.3.2"]
    [org.clojure/data.csv "0.1.4"]
    [org.clojure/data.json "0.2.6"]
    [org.clojure/data.zip "0.1.2"]
    [org.clojure/core.match "0.3.0"]
    [dk.ative/docjure "1.12.0"]
   ]
  :plugins [
            [lein-virgil "0.1.9"]
            ]
  :main clojucture.core
  :mirrors {"central" {:name "aliyun maven"
                       :url "https://maven.aliyun.com/nexus/content/groups/public/"}}
  ;:user {:plugins [ [nightlight/lein-nightlight "RELEASE"]]}
  :source-paths ["src/clj"]
  :java-source-paths [ "src/java" ]
  :profiles {
   :uberjar {:aot :all}
   :dev {
    :plugins [
      [ lein-shell "0.5.0"]
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
)
