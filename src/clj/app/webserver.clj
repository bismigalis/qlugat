(ns app.webserver
  ;;(:gen-class) ; for -main method in uberjar
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [com.stuartsierra.component :as c]
            [app.views :as views]
            [app.interceptors :as interceptors]

            #_[lib.webjars :as webjars]))

(defn insert [vec pos item]
  (apply conj (subvec vec 0 pos) item (subvec vec pos)))

(defn my-default-interceptors
  [service-map]
  (update-in service-map [:io.pedestal.http/interceptors]
             #(vec (-> %
                        (insert 4 (body-params/body-params))
                        ))))


(def routes (route/expand-routes
  #{["/"           :get [views/index-page] :route-name :home]
    ["/get_json"   :get [views/get-json] :route-name :get-json]
    ["/suggest"    :get [views/suggest-handler] :route-name :suggest-handler]
    ["/edit_word"  :put [interceptors/auth views/put-word] :route-name :put-word]
    ["/login"      :get  views/index-page :route-name :login]
    ["/login"      :post [views/login-post] :route-name :login-post]
    ["/app.js"     :get [views/serve-main-js] :route-name :main-js]
    }))

(def service {:env                  :prod
              ::http/secure-headers {:content-security-policy-settings {}
                                     :frame-options-settings "ALLOW"}
              ::http/routes         routes
              ::http/file-path      (str (System/getProperty "user.dir") "/js")
              ::http/type           :jetty
              ::http/port           8080
              ::http/host           (or (System/getenv "OPENSHIFT_DIY_IP") "localhost")})

(defn run-dev
  [& args]
  (println "\nCreating your [DEV] server...")
  (-> service ;; start with production configuration
      (merge {:env :dev
              ;; do not block thread that starts web server
              ::http/join? false
              ;; Routes can be a function that resolve routes,
              ;;  we can use this to set the routes to be reloadable
              ::http/routes #(deref #'routes)
              ;; all origins are allowed in dev mode
              ::http/allowed-origins {:creds true :allowed-origins (constantly true)}
              ;;::http/secure-headers {:object-src "'none'" :default-src "'self'"}
              })
      http/default-interceptors
      my-default-interceptors
      http/dev-interceptors
      http/create-server
      http/start))

(defn start-prod
  [& args]
  (println "\nCreating your [PROD] server...")
  (-> service ;; start with production configuration
      http/default-interceptors
      ;;my-default-interceptors
      http/create-server
      http/start))

;; For interactive development
;;(defonce server (atom nil))

(defrecord Webserver []
  c/Lifecycle

  (start [component]
    (println "Starting webserver")
    (assoc component :server (if (= :prod (:type component))
                               (start-prod)
                               (run-dev)))
    )

  (stop [component]
    (println "Stopping webserver")
    (http/stop (:server component))
    (dissoc component :server)
    )
  )

(defn new-webserver [config]
  (map->Webserver {:type (:type config)})
  )
