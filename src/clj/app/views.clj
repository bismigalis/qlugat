(ns app.views
  (:require ;;[net.cgrand.enlive-html :as e]
            ;;[net.thegeez.w3a.context :as ctx]
            ;;[net.thegeez.w3a.link :as lnk]
            [lib.form :as f]
            [lib.util :as util]
            [app.stemmer :refer [detect-lang get-stem]]
            [app.forms]
            [datomic.api :as d]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [hiccup.core :refer [html]]
            )
  (:use [clojure.tools.logging]
        ;;[clj-logging-config.log4j]
        )
  )
(def users {"ruslan" "f00d644181e880b6107c06a4750cc74f"})
#_(set-config-logging-level! :debug)
#_(set-logger! :name "file"
             :out (org.apache.log4j.FileAppender. (org.apache.log4j.SimpleLayout.)
                                                  "/tmp/app.log"
                                                  true)
             :level :debug
             )


(def db-uri (or (System/getenv "DATOMIC_URL") "datomic:dev://localhost:4334/firstdb"))
(def conn (d/connect db-uri))

(defn put-word [request]
  (let [conn (d/connect db-uri)
        {:keys [id article]} (:json-params request)]
    #_@(d/transact conn [#_[:db/add id :word/word word]
                       [:db/add id :article/article article]
    ])
    )
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/write-str {})
   }
  )


(defn get-json
  [request]
  (let [word (get-in request [:params :word] "")
        res (d/q '[:find [(pull ?e [*]) ...]
                                               :in $ ?word
                                               :where [?e :word/word ?word]]
                                             (d/db conn)
                                             word)]
    (if (empty? res)
      {:status 404
       :headers {"Content-Type" "application/json; charset=utf-8"}
       :body (json/write-str {:message "Word not found"})}
      {:status 200
       :headers {"Content-Type" "application/json; charset=utf-8"}
       :body (json/write-str (first res))}
      )))

(defn suggest-handler [request]
  (let [token (get-in request [:params :token] "")
        stem (get-stem token)
        lang (case (detect-lang token) :ru :ru-crh :crh :crh-ru)]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str (->> (d/q '[:find [(pull ?e [:word/stem :word/word]) ...]
                                       :in $ ?lang
                                       :where
                                       [?e :word/dictionary ?lang]
                                       ]
                                     (d/db conn)
                                     lang)
                                (filter #(clojure.string/starts-with? (:word/stem %) stem))
                                (map :word/word)
                                ;;(map #(str/replace % "'" "\u0301"))
                                sort
                                dedupe
                                vec))}))

#_(defn get-word [request]
    (let []
      (d/q '[:find ?e :where [?e :word/stem "bal"]] (d/db conn))
      ))


#_(defn login-page
  [request]
  (let []
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (apply str (login request))}))



(defn index-page
  [request]
  (let []
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (html
            [:html
             [:head
              [:meta {:charset "utf-8"}]
              [:link {:rel :stylesheet
                      :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"}]]
             [:body
              [:div {:id "container"}]
              [:script {:src "js/main.js"}]
              ]])}))



(defn valid-credentials? [data]
  (= (get users (:username data))
     (util/md5 (:password data)))
  )

(defn login-post
    [request]
    (let [data (:json-params request)]
      (if (valid-credentials? data)
        {:status 200
         :headers {"Content-Type" "application/json; charset=utf-8"}
         :body (json/write-str {:token (get users (:username data))})}
        {:status 401
         :headers {"Content-Type" "application/json; charset=utf-8"}
         :body (json/write-str {:error "Login failed"})}
        )
      ))
