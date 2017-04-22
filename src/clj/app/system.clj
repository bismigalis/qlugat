(ns app.system
  (:require [com.stuartsierra.component :as c]
            [app.webserver :refer [new-webserver]]
            [app.sqldatabase :refer [new-sqldatabase]]
            [app.config :refer [dev-config prod-config]]
            ))


#_(defrecord Database [host port connection]
  c/Lifecycle

  (start [component]
    (println ";; Starting database")
    (let [conn (connect-to-database host port)]
      ;; Return an updated version of the component with
      ;; the run-time state assoc'd in.
      (assoc component :connection conn)))

  (stop [component]
    (println ";; Stopping database")
    (.close connection)
    ;; Return the component, optionally modified. Remember that if you
    ;; dissoc one of a record's base fields, you get a plain map.
    (assoc component :connection nil)))

#_(defn new-database [config]
  (map->Database {:host host :port port}))




(defn make-system [config]
  (let []
    (c/system-map
     ;;:db (new-sqldatabase config)
     ;;:web (c/using (new-webserver config) [:db])
     :web (new-webserver config)
     )))

(defn make-dev-system []
  (make-system dev-config))

(defn make-prod-system []
  (make-system prod-config))


(defn start [system]
  (c/start system))

(defn stop [system]
  (c/stop system))
