(ns app.system
  (:require [com.stuartsierra.component :as c]
            [app.webserver :refer [new-webserver]]
            [app.sqldatabase :refer [new-sqldatabase]]
            [app.logdb :refer [new-logdb]]
            [app.config :refer [dev-config prod-config]]
            ))


(defn make-system [config]
  (let []
    (-> (c/system-map
         ;;:db (new-sqldatabase config)
         :web (new-webserver config)
         :logdb (new-logdb config)
         )
        (c/system-using
           {:web [:logdb]
            })
        )))


(defn make-dev-system []
  (make-system dev-config))

(defn make-prod-system []
  (make-system prod-config))


(defn start [system]
  (c/start system))

(defn stop [system]
  (c/stop system))
