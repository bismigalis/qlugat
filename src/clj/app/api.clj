(ns app.api
  (:require [clojure.string :as str]
            [app.stemmer :refer [detect-lang get-stem]]
            [jdbc.core :as jdbc])
  (:import [com.sleepycat.je DatabaseEntry DiskOrderedCursor DiskOrderedCursorConfig LockMode OperationStatus]))


(defn sql-get-nearest-word [word]
  (let [stem (get-stem word)
        stems (map #(subs %1 0 %2) (repeat stem) (range (count stem) 0 -1))]
    (into [(str "SELECT * FROM word WHERE "
                (->> stems
                     (map (constantly "stem=?"))
                     (interpose "OR")
                     (clojure.string/join " ")))]
          stems)))

(defn lengthiest [words]
  (def WORDS words)
  (first (sort-by :word #(compare (count %2) (count %1)) words)))

(defn get-word-crh [dbspec word]
  (with-open [conn (jdbc/connection dbspec)]
    (if-let [word (lengthiest (jdbc/fetch conn (sql-get-nearest-word word)))]
      (assoc word
             :articles
             (jdbc/fetch conn ["SELECT * FROM article WHERE word_id = ?" (:id word)])))))

(defn get-word-ru [dbspec word]
  (with-open [conn (jdbc/connection dbspec)]
    (if-let [word (first (jdbc/fetch conn ["SELECT * FROM word WHERE word = ?" word]))]
      (assoc word
             :articles
             (jdbc/fetch conn ["SELECT * FROM article WHERE word_id = ?" (:id word)])))))


(defn get-word [dbspec word]
  (if (= (detect-lang word) :crh)
    (get-word-crh dbspec word)
    (get-word-ru  dbspec word)
  ))


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

(defn log-word [db word found-word]
  (let [key  (new DatabaseEntry (.getBytes word "UTF-8"))
        data (new DatabaseEntry (.getBytes (str found-word) "UTF-8"))]
    (.put db nil key data)
    ))

(defn- get-next [cur]
  (let [key (new DatabaseEntry)
        data (new DatabaseEntry)
        result (.getNext cur key data LockMode/READ_UNCOMMITTED)]
    (if (= result OperationStatus/SUCCESS)
      [(new String (.getData key) "UTF-8")
       (new String (.getData data) "UTF-8")]
      )))

(defn get-missed-words [db]
  (let [cur (.openCursor db (new DiskOrderedCursorConfig))]
    (loop [result []]
      (if-let [item (get-next cur)]
        (recur (conj result item))
        (do (.close cur)
            result)
      ))
  ))
