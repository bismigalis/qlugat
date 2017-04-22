(ns app.api
  (:require [clojure.string :as str]
            [app.stemmer :refer [get-stem]]
            [jdbc.core :as jdbc]))

(defn get-word [dbspec word]
  (with-open [conn (jdbc/connection dbspec)]
    (if-let [word (first (jdbc/fetch conn ["SELECT * FROM word WHERE word = ?" word]))]
      (assoc word
             :articles
             (jdbc/fetch conn ["SELECT * FROM article WHERE word_id = ?" (:id word)])))))


(defn get-suggestions [dbspec lang letter]
  (with-open [conn (jdbc/connection dbspec)]
    (jdbc/fetch conn ["SELECT word, stem FROM word WHERE dict=? AND LEFT(stem,1)=? ORDER BY word" lang letter])))


(defn put-article [dbspec id article]
  (with-open [conn (jdbc/connection dbspec)]
    (jdbc/execute conn ["UPDATE article SET article=? WHERE id=?" article id])))

;; ;; USER
;; (defn make-user [name email password]
;;   {:user/name name
;;    :user/email email
;;    :user/encrypted-password password})

;; (defn get-user-by-email-and-password [db email password]
;;   (d/q '[:find ?e
;;          :where
;;          [?e :user/email email]
;;          [?e :user/encrypted-password (encript password)]
;;          ] db))

(defn get-shortening-pos [word]
  (str/index-of word "|")
  )
;; ;; WORD
;; (defn make-word [word articles dictionary]
;;   {:word/word word
;;    :word/stem (get-stem word)
;;    :word/dictionary dictionary
;;    :word/shorteningPos (get-shortening-pos word)
;;    :word/articles articles
;;    })
