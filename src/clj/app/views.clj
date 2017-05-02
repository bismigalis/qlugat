(ns app.views
  (:require [clojure.java.io :as io]
            [lib.form :as f]
            [lib.util :as util]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [hiccup.core :refer [html]]
            [app.stemmer :refer [detect-lang get-stem]]
            [app.forms]
            [app.api :as api]
            [app.config :refer [prod-config]]
            )
  (:use [clojure.tools.logging]
        ;;[clj-logging-config.log4j]
        )
  )
(def users {"ruslan" "f00d644181e880b6107c06a4750cc74f"})

(defn put-word [request]
  (let [{:keys [id article]} (:json-params request)]
    (api/put-article (:dbspec prod-config) id article))
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/write-str {})
   }
  )


(defn get-json
  [request]
  (let [word (get-in request [:params :word] "")
        found-word (api/get-word (:dbspec prod-config) word)]
    (if-not (= word found-word)
      (api/log-word (:logdb request) word (:word found-word)))
    (if (empty? found-word)
      (do
        {:status 404
         :headers {"Content-Type" "application/json; charset=utf-8"}
         :body (json/write-str {:message "Word not found"})})

      {:status 200
       :headers {"Content-Type" "application/json; charset=utf-8"}
       :body (json/write-str found-word)}
      )))


(defn suggest-handler [request]
  (let [token (get-in request [:params :token] "")
        stem  (get-stem token)
        lang  (case (detect-lang token) :ru "ru-crh" :crh "crh-ru")
        res (api/get-suggestions (:dbspec prod-config) lang (first stem))]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str (map :word res))}
))

(defn missed-words [request]
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/write-str {:words (api/get-missed-words (:logdb request))})
   })

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
                      :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"}]
              [:style "* {font-family: monospace;}\n dl {margin-bottom:0;}"]
              ]
             [:body
              [:div {:id "container"}]
              [:script {:src "app.js"}]
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
         :headers {"Content-Type" "application/json; charset=utf-8"
                   "Set-Cookie" (str "auth-token=" (get users (:username data)))}
         :body (json/write-str {:token (get users (:username data))})}
        {:status 401
         :headers {"Content-Type" "application/json; charset=utf-8"}
         :body (json/write-str {:error "Login failed"})}
        )
      ))


(defn serve-main-js [request]
  (if (.exists (io/as-file (str (System/getProperty "user.dir") "/js/main.js.gz")))
    {:status 200
     :headers {"Content-Type" "text/javascript"
               "Content-Encoding" "gzip"}
     :body (io/file "js/main.js.gz")
     }
    {:status 200
     :headers {"Content-Type" "text/javascript"}
     :body (io/file "js/main.js")}
    ))
