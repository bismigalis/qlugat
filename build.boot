(set-env!
 :source-paths #{"src/clj" "src/cljs" "src/cljc" "resources" "config"}
 ;;:resource-paths #{"resources" "config"}
 :repositories #(conj % ["my.datomic.com" {:url "https://my.datomic.com/repo"
                                           :username "r.prokopiev@gmail.com"
                                           :password "822e975a-e02c-4779-ae67-863fede4c218"}])
 :dependencies '[[com.datomic/datomic-pro "0.9.5544"]
                 [io.pedestal/pedestal.service "0.5.2"]
                 [io.pedestal/pedestal.jetty "0.5.2"]
                 [io.pedestal/pedestal.interceptor "0.5.2"]
                 ;;[io.pedestal/pedestal.log "0.5.2"]
                 [geheimtur "0.3.0"]
                 ;;[enlive "1.1.6"]
                 [hiccup "1.0.5"]
                 [org.webjars/webjars-locator "0.27"]
                 [org.webjars/bootstrap "3.3.7"]

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
                 [org.clojure/core.async      "0.3.441"]
                 [org.clojure/data.json "0.2.6"]

                 [crisptrutski/boot-cljs-test "0.3.0"]
                 [com.google.guava/guava "21.0"]
                 ])



(require '[boot.cli :refer [defclifn]]
         '[datomic.api :as d :refer [q db]]
         '[clojure.core.async :refer (<!!)]
         '[clojure.data.csv :as csv]
         '[clojure.java.io :as io]
         '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl cljs-repl-env start-repl]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-test :refer :all]
         '[pandeiro.boot-http :refer [serve]]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]]

         '[clojure.string :as str]
         '[app.api :as api]
         '[app.stemmer :refer [get-stem]]
         '[app.server]
         )
(def db-uri "datomic:dev://localhost:4334/firstdb")

(deftask createdb
  "t"
  []
  (d/create-database db-uri))

#_(deftask import
  ""
  [e entity VAL kw "entity"]
  (print VAL))

(deftask frontend []
  (comp
        ;;(watch "-i" #"\.cljs$")
        (watch)
        ;;      (show "-f")
        (reload "-c" "js")
        (cljs-repl)
        (cljs)
        (speak)
        (target "-d" "js")
  ))

(deftask build-cljs []
  ;;(set-env! :source-paths #(conj % "src-cljs-prod"))
  (comp (cljs :optimizations :advanced)
        (target "-d" "js")))

(deftask start-server
  "A post task."
  []
  (let []
    (with-post-wrap fileset
      (app.server/start-prod)
      )))

(deftask prod []
  (comp
   ;; (build-cljs)
   ;; (target "-d" "js")
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



(def crh-file "/home/user/workspace/lugat/old/crh_ru.csv")
(def ru-file "/home/user/workspace/lugat/new/ru_crh.txt")

(defn import-crh
  ""
  []
  (let [conn (d/connect db-uri)]

    #_(with-open [file (io/reader crh-file)]
    (doseq [[word article] (rest (csv/read-csv file))]
      @(d/transact conn [(api/make-word word article :crh-ru)])
      ))
    (with-open [file (io/reader crh-file)]
      @(d/transact conn (vec (map (fn [[word article]]
                                        ;(api/make-word word article :crh-ru)
                                    {:word/word word
                                     :word/stem (get-stem word)
                                     :word/dictionary :crh-ru
                                     :word/articles [{:article/article article}]}
                                    )
                                  (rest (csv/read-csv file)))))
      )

  ))

#_(defn retract-ru []
  (let [conn (d/connect db-uri)
        ids (d/q '[:find [?e ...] :where [?e :word/dictionary :ru-crh]] (d/db conn))]
    @(d/transact conn (->> ids
                           (map (fn [x] [:db.fn/retractEntity x]))
                           vec))
    ))


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

(defn import-ru
  ""
  []
  (with-open [file (io/reader ru-file)]
    (let [conn (d/connect db-uri)]
      (reduce (fn [a [raw-word article _]]
                      (let [word (get-word raw-word)
                            stem (get-stem word)
                            accentPos (get-accent-pos raw-word)
                            shorteningPos (api/get-shortening-pos raw-word)
                            ]
                        (if (contains? a word)
                          (update-in a [word :word/articles] conj (assoc-if {:article/article article}
                                                                       :article/accentPos
                                                                       accentPos))
                          (assoc a word (-> {:word/word word
                                             :word/stem stem
                                             :word/dictionary :ru-crh
                                             :word/articles [(assoc-if {:article/article article}
                                                                       :article/accentPos
                                                                       accentPos)]


                                             }
                                            (assoc-if :word/shorteningPos shorteningPos)
                                            ))
                        )))
                    {}
                    (partition 3 (line-seq file))))

    )
      #_@(d/transact conn (->>

                             (map (fn [[word article _]] (api/make-word word article :ru-crh)))
                             vec)
                   )
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

  #_@(d/transact conn
                 (->> items
                      (filter #(str/ends-with? (:word/word %) " IV"))
                      (map #(vector (:db/id %) (:word/word %)))
                      ;;(map (fn [[id word]] [id word (str/index-of word "|")]))
                      (map (fn [[id word]]
                             [[:db/add id :word/variant 4]
                              [:db/retract id :word/word word]
                              [:db/add id :word/word (str/replace word " IV" "")]]
                             ))
                      (apply concat)

                      )
                 )



  #_@(d/transact conn
                 (->> items
                      (filter #(:word/accentPos %))

                      (map (fn [{id :db/id pos :word/accentPos}]
                             [[:db/retract id :word/accentPos pos]
                              [:db/add id :word/accentPos (dec pos)]]
                             ))
                      (apply concat)

                      )
                 )
  )


(defclifn -main
  [a awesome bool "Whether you want this app to be awesome or not. (Default true)"]
  (println "Named parameters " *opts*)
  (println "List of arguments " *args*))
