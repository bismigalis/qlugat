(ns app.api
  (:require [datomic.api :as d]
            [app.stemmer :refer [get-stem]]
            [clojure.string :as str])
  )
;; USER
(defn make-user [name email password]
  {:user/name name
   :user/email email
   :user/encrypted-password password})

(defn get-user-by-email-and-password [db email password]
  (d/q '[:find ?e
         :where
         [?e :user/email email]
         [?e :user/encrypted-password (encript password)]
         ] db))

(defn get-shortening-pos [word]
  (str/index-of word "|")
  )
;; WORD
(defn make-word [word articles dictionary]
  {:word/word word
   :word/stem (get-stem word)
   :word/dictionary dictionary
   :word/shorteningPos (get-shortening-pos word)
   :word/articles articles
   })
