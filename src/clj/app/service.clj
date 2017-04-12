(ns app.service
  (:require [clojure.java.io :as io]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor.chain :refer [terminate]]
            [app.views :as views]
            [clojure.data.json :as json]
            [geheimtur.impl.form-based :refer [default-login-handler default-logout-handler]]
            )
  )



#_(def users {"user" {:name "user"
                    :password "password"
                    :roles #{:user}
                    :full-name "Bobby Briggs"}
            "admin" {:name "admin"
                     :password "password"
                     :roles #{:admin :agent}
                     :full-name "Dale Cooper"}})

#_(defn credentials
  [_ {:keys [username password]}]
  (when-let [identity (get users username)]
    (when (= password (:password identity))
      (dissoc identity :password ))))

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

(defn serve-main-js [request]
  {:status 200
   :headers {"Content-Type" "text/javascript"
             "Content-Encoding" "gzip"}
   :body (io/file "js/main.js.gz")
  })

(def routes (route/expand-routes
  #{["/"           :get [views/index-page] :route-name :home]
    ["/get_json"   :get [views/get-json] :route-name :get-json]
    ["/suggest"    :get [views/suggest-handler] :route-name :suggest-handler]
    ["/edit_word"  :put [auth views/put-word] :route-name :put-word]
    ["/login"      :get  views/index-page :route-name :login]
    ["/login"      :post [views/login-post] :route-name :login-post]
    ["/js/main.js" :get [serve-main-js] :route-name :main-js]
    }))


#_(defroutes routes
  [[["/" {:get views/home-page}
     ^:interceptors [(body-params/body-params)
                     bootstrap/html-body
                     session-interceptor]
     ["/login" {:get views/login-page
                :post (default-login-handler {:credential-fn credentials})}]
     ["/logout" {:get default-logout-handler}]
     ["/interactive" {:get views/interactive-index} ^:interceptors [access-forbidden-interceptor (interactive {})]
      ["/restricted" {:get views/interactive-restricted} ^:interceptors [(guard :silent? false)]]]]]])



(def service {:env                 :prod
              ::http/secure-headers {:content-security-policy-settings {}}
              ::http/routes        routes
              ::http/resource-path "/public"
              ::http/type          :jetty
              ::http/port 8080})
