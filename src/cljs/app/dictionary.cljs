(ns app.dictionary
  (:require [app.stemmer :refer [get-stem]]
            [clojure.string :as str]))


(defn set-list-by-letter [m letter words]
  (assoc m letter (->> words
                       (map (fn [x] {:word x :stem (get-stem x)}))
                       (sort-by :stem)
                       vec)))

(defn get-list-by-token [m token]
  (->> (get m (first token))
       (filter (fn [{v :stem}] (str/starts-with? v token)))
       ;;(map :word)
       vec
      ))
