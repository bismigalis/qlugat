(ns app.config)

(def dev-config {:type :dev
                 :dbspec  {:classname   "org.h2.Driver"
                          :subprotocol "h2:file"
                          :subname     (str (System/getProperty "user.dir") "/data/database")
                          :user        "sa"
                          :password    ""
                           }

                 })

(def prod-config {:type :prod
                  :dbspec  {:classname   "org.h2.Driver"
                            :subprotocol "h2:file"
                            :subname     (str (System/getenv "OPENSHIFT_DATA_DIR") "database")
                            :user        "sa"
                            :password    ""
                            }
                  })
