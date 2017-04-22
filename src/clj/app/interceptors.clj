(ns app.interceptors
  (:require [clojure.data.json :as json]
            [io.pedestal.interceptor.chain :refer [terminate]]
            [app.views :as views]
            ))

(def auth
  {:name ::auth
   :enter (fn [context]
            (let [request (:request context)
                  response {:status 403
                            :headers {"Content-Type" "application/json; charset=utf-8"}
                            :body (json/write-str {})
                            }]
              ;;(def REQ request)
              (if-not ((set (vals views/users)) (-> request :headers (get "token")))
                (terminate (assoc context :response response))
                context
                )))})

#_(def attach-system-to-request
  {:name ::system
   :enter (fn [context]
            (let [request (:request context)
                  ]
              (assoc-in context [:request ::system] system)))})
