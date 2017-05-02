(ns app.config)

(defn get-db []
  (if (System/getenv "OPENSHIFT_DATA_DIR")
    (str (System/getenv "OPENSHIFT_DATA_DIR") "database")
    (str (System/getProperty "user.dir") "/data/database")
    ))

(def dev-config {:type :dev
                 :dbspec  {:classname  "org.h2.Driver"
                          :subprotocol "h2:file"
                          :subname     (get-db)
                          :user        "sa"
                          :password    ""
                           }
                 :logdb {:path "/tmp"
                          :dbname "logdb"}

                 })

(def prod-config {:type :prod
                  :dbspec  {:classname   "org.h2.Driver"
                            :subprotocol "h2:file"
                            :subname     (get-db)
                            :user        "sa"
                            :password    ""
                            }
                  :logdb {:path (or (System/getenv "OPENSHIFT_DATA_DIR") "/tmp")
                          :dbname "logdb"}
                  })
