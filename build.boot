(set-env!
 :source-paths #{"src/clj" "src/cljs" "src/cljc" "resources" "config"}
 ;;:resource-paths #{"resources" "config"}
 :dependencies '[[io.pedestal/pedestal.service "0.5.2"]
                 [io.pedestal/pedestal.jetty "0.5.2"]
                 [io.pedestal/pedestal.interceptor "0.5.2"]
                 ;;[io.pedestal/pedestal.log "0.5.2"]
                 ;;[geheimtur "0.3.0"]
                 [hiccup "1.0.5"]

                 ;;[clj-logging-config "1.9.12"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 ;;[org.slf4j/slf4j-log4j12 "1.7.22"]
                 #_[log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [org.clojure/data.csv "0.1.3"]

                 [adzerk/boot-cljs        "2.0.0" :scope "test"]
                 [adzerk/boot-reload      "0.5.1" :scope "test"]
                 [adzerk/boot-cljs-repl   "0.3.3"] ;; latest release
                 [adzerk/boot-test "1.2.0"]
                 [pandeiro/boot-http "0.7.6"]

                 [weasel                  "0.7.0"  :scope "test"]
                 [com.cemerick/piggieback "0.2.1"  :scope "test"]
                 [org.clojure/tools.nrepl "0.2.12" :scope "test"]

                 ;;[org.clojure/clojurescript "1.9.495"]
                 [org.clojure/clojurescript "1.9.229"]
                 [reagent "0.6.0"]
                 [re-frame "0.9.2"]
                 [org.clojure/core.async      "0.3.441"]
                 [org.clojure/data.json "0.2.6"]

                 [crisptrutski/boot-cljs-test "0.3.0"]
                 [com.google.guava/guava "21.0"]

                 [com.stuartsierra/component "0.3.2"]
                 [ragtime "0.7.1"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [samestep/boot-refresh "0.1.0" :scope "test"]
                 [com.h2database/h2 "1.4.191"]
                 [com.sleepycat/je "5.0.73"]
                 [org.clojure/java.jdbc "0.7.0-alpha3"]
                 [funcool/clojure.jdbc "0.9.0"]
                 [spyscope "0.1.6"]
                 ])



(require '[boot.cli :refer [defclifn]]
         ;;'[clojure.core.async :refer (<!!)]
         '[clojure.data.csv :as csv]
         '[clojure.java.io :as io]
         '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl cljs-repl-env start-repl]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-test :refer :all]
         '[pandeiro.boot-http :refer [serve]]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]]
         '[clojure.java.shell :as shell]
         '[clojure.string :as str]
         '[clojure.tools.namespace.repl :refer [refresh set-refresh-dirs]]
         ;;'[samestep.boot-refresh :refer [refresh]]
         '[app.api :as api]
         '[app.stemmer :refer [get-stem]]
         '[app.system]
         ;;'[app.server]
         '[ragtime.jdbc :as rjdbc]
         '[jdbc.core :as jdbc]
         '[ragtime.repl :refer [migrate]]
         '[app.config :refer [dev-config]]
         '[spyscope.core]
         )

(boot.core/load-data-readers!)

(def dirs (get-env :directories))
(apply set-refresh-dirs dirs)


(def ragtime-config {:datastore (rjdbc/sql-database (:dbspec dev-config))
                     :migrations (rjdbc/load-resources "migrations")})


(def system nil)

;; (defn start []
;;   (alter-var-root #'my-app (constantly (start-my-app))))


(defn init-system
  "Constructs the current development system."
  []
  (alter-var-root #'system (constantly (app.system/make-dev-system))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system app.system/start))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system (fn [s] (when s (app.system/stop s)))))

(defn reset []
  (stop)
  (refresh :after `start))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init-system)
  (start))



(deftask frontend []
  (comp (watch)
        (reload)
        (cljs-repl-env)
        (cljs)
        (speak)
        (target "-d" "js")
  ))

(deftask gzip-main-js
  "A post task."
  []
  (let [tmp (tmp-dir!)]
    (def tmp tmp)
    (with-pre-wrap fileset
      (let [f (-> fileset :tree (get "main.js"))]
        (def f f)
        (shell/sh "cp"
                  (.getAbsolutePath (tmp-file f))
                  (.getAbsolutePath (io/file tmp "main.js"))
                  )
        (shell/sh "gzip" (.getAbsolutePath (io/file tmp "main.js")))
        (commit! (add-asset fileset tmp))
        ;;fileset
        )
      ))
  )


(deftask build-cljs []
  ;;(set-env! :source-paths #(conj % "src-cljs-prod"))
  (comp (cljs :optimizations :advanced)
        (gzip-main-js)
        (target "-d" "js")))

(deftask start-prod-server
  "A post task."
  []
  (let []
    (with-post-wrap fileset
      ;;(app.server/start-prod)
      (app.system/start (app.system/make-prod-system))
      )))

(deftask start-server
  "A post task."
  []
  (let []
    (with-post-wrap fileset
      ;;(app.server/start-prod)
      (app.system/start (app.system/make-dev-system))
      )))

(deftask prod []
  (comp
   (build-cljs)
   (start-server)
  ))


(deftask testing []
  (merge-env! :source-paths #{"test/frontend"})
  identity)

(deftask test-frontend []
  (comp (watch)
        (testing)
        (test-cljs)
        (speak "-t" "woodblock"))
  )


#_(deftask build []
  (comp
   (build-cljs)
   (aot :namespace '#{animals.uberjar})
   (pom :project 'animals
        :version "1.0.0")
   (uber)
   (jar :main 'animals.uberjar)))

(comment
(def crh-file "/home/user/workspace/lugat/old/crh_ru.csv")
(defn import-crh
  ""
  []
  (with-open [conn (jdbc/connection app.system/dbspec)]
      (with-open [file (io/reader crh-file)]
        (doseq [[idx [word article]] (map-indexed vector (rest (csv/read-csv file)))]
          (jdbc/execute conn ["INSERT INTO word (id, word, stem, dict, shortening_pos) VALUES (?, ?, ?, ?, ?);" idx word (get-stem word) "crh-ru" nil])
          (jdbc/execute conn ["INSERT INTO article (id, word_id, accent_pos, article) VALUES (?, ?, ?, ?);" idx idx nil article])
          ))
  ))
)

(defn get-word [raw]
  (-> raw
      (str/replace #" I+V?$" "")
      (str/replace "'" "")
      (str/replace "|" "")
      )
  )
(defn get-accent-pos [raw]
  (-> raw
      (str/replace "|" "")
      (str/index-of "'")
      ))
(defn assoc-if
  "assoc key/value pairs to the map only on non-nil values
   (assoc-if {} :a 1)
   => {:a 1}
   (assoc-if {} :a 1 :b nil)
   => {:a 1}"
  {:added "2.1"}
  ([m k v]
   (if (not (nil? v)) (assoc m k v) m))
  ([m k v & more]
   (apply assoc-if (assoc-if m k v) more)))

(def ru-file "/home/user/workspace/lugat/new/ru_crh.txt")
(defn import-ru
  ""
  []
  (with-open [conn (jdbc/connection (:dbspec dev-config))]
    (with-open [file (io/reader ru-file)]
      (let [data (reduce (fn [a [raw-word article _]]
                           (let [word (get-word raw-word)
                                 stem (get-stem word)
                                 accent-pos (get-accent-pos raw-word)
                                 shortening-pos (api/get-shortening-pos raw-word)
                                 ]
                             (if (contains? a word)
                               (update-in a [word :articles] conj (assoc-if {:article article}
                                                                            :accent_pos
                                                                            accent-pos))
                               (assoc a word (-> {:word word
                                                  :stem stem
                                                  :dict "ru-crh"
                                                  :articles [(assoc-if {:article article}
                                                                       :accent_pos
                                                                       accent-pos)]


                                                  }
                                                 (assoc-if :shortening_pos shortening-pos)
                                                 ))
                               )))
                         {}
                         (partition 3 (line-seq file)))]
        ;;data
        (doseq [[idx [word word-entry]] (map-indexed vector data)]
          (let [idx (+ 20000 idx)]
            (jdbc/execute conn ["UPDATE word SET shortening_pos=? WHERE id=?" (:shortening_pos word-entry) idx])
            #_(jdbc/execute conn ["INSERT INTO word (id, word, stem, dict, shortening_pos) VALUES (?, ?, ?, ?, ?);" idx word (get-stem word) "ru-crh" (:shortening_pos word-entry)])
            #_(doseq [{:keys [article accent_pos]}  (:articles word-entry)]
              (jdbc/execute conn ["INSERT INTO article (word_id, accent_pos, article) VALUES (?, ?, ?);" idx accent_pos article])))
          )
      )))
  )

(comment
  (def conn (d/connect db-uri))
  (def items (d/q '[:find [(pull ?e [*]) ...] :where [?e :word/dictionary :ru-crh]] (d/db conn)))



  (->> items
       (filter #(str/includes? (:word/stem %) " "))
       )
  #_(let [id 17592186099725]
    @(d/transact conn
                 [[:db/retract id :word/stem "стан iv"]
                  [:db/add id :word/stem "стан"]]))

  )


(defclifn -main
  [a awesome bool "Whether you want this app to be awesome or not. (Default true)"]
  (println "Named parameters " *opts*)
  (println "List of arguments " *args*))



#_(defn getEntry [str]
  (new DatabaseEntry (.getBytes str "UTF-8")))
