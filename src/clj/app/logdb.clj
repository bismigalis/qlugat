(ns app.logdb
  (:require [com.stuartsierra.component :as c])
  (:import [com.sleepycat.je Database DatabaseConfig DatabaseException Environment EnvironmentConfig]))



(defrecord LogDatabase [path dbname]
  c/Lifecycle

  (start [this]
    (let [env-config (doto (new EnvironmentConfig) (.setAllowCreate  true))
          env (Environment. (clojure.java.io/file path) env-config)
          db-config (doto (new DatabaseConfig) (.setAllowCreate  true))
          db (.openDatabase env nil dbname db-config)]
      (assoc this :env env :db db)
      )
    )

  (stop [this]
    (.close (:db this))
    (.close (:env this))
    (dissoc this :db :env)
    )
  )

(defn new-logdb [config]
  (map->LogDatabase (:logdb config)))
