(ns app.server
  ;;(:gen-class) ; for -main method in uberjar
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [app.service :as service]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [lib.webjars :as webjars]))

(defn insert [vec pos item]
  (apply conj (subvec vec 0 pos) item (subvec vec pos)))

(def bootstrap-webjars-resource-path "META-INF/resources/webjars/bootstrap/3.3.7")

(defn my-default-interceptors
  [service-map]
  (update-in service-map [:io.pedestal.http/interceptors]
             #(vec (-> %
                        ;;(cons (webjars/make-resource-handler))
                        (insert 4 (webjars/make-resource-handler))
                        (insert 4 (body-params/body-params))
                        ))))
(defn run-dev
  [& args]
  (println "\nCreating your [DEV] server...")
  (-> service/service ;; start with production configuration
      (merge {:env :dev
              ;; do not block thread that starts web server
              ::http/join? false
              ;; Routes can be a function that resolve routes,
              ;;  we can use this to set the routes to be reloadable
              ::http/routes #(deref #'service/routes)
              ;; all origins are allowed in dev mode
              ::http/allowed-origins {:creds true :allowed-origins (constantly true)}
              ;;::http/secure-headers {:object-src "'none'" :default-src "'self'"}
              })
      http/default-interceptors
      my-default-interceptors
      ;;http/dev-interceptors
      http/create-server
      http/start))

(defn start-prod
  [& args]
  (println "\nCreating your server...")
  (-> service/service ;; start with production configuration
      http/default-interceptors
      my-default-interceptors
      http/create-server
      http/start))

;; For interactive development
(defonce server (atom nil))

(defn start-dev []
  (reset! server (run-dev)
          #_(http/start (http/create-server
                         (assoc service/service ::http/join? false)))
          ))

(defn stop-dev []
  (http/stop @server))

(defn restart []
  (stop-dev)
  (start-dev))
