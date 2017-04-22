(ns app.sqldatabase
  (:require [clojure.java.jdbc :as j]
            [com.stuartsierra.component :as c]))



(defrecord SQLDatabase [classname subprotocol subname user password]
  ;; c/Lifecycle

  ;; (start [this]
  ;;   )

  ;; (stop [this]
  ;;   )
  )

(defn new-sqldatabase [config]
  (map->SQLDatabase (:sql-database config)))
