(ns app.stemmer
  (:require [clojure.string :as str]))

(def rus-letters "абвгдеёжзийклмнопрстуфхцчшщьыъэюя")

(defn detect-lang [txt]
  (if (str/index-of rus-letters (first txt))
    :ru
    :crh))


(defmulti get-stem detect-lang)

(defmethod get-stem :ru [word]
  (-> word
      (str/replace #" I+V?$" "")
      (str/replace "'" "")
      (str/replace "|" "")
      (str/lower-case)
      (str/replace "ё" "е")
      ))

(defmethod get-stem :crh [word]
  (let [notverbs #{"otmek"}
        notverb #(contains? notverbs word)
        ]
    (-> word
        (str/lower-case)
        ((fn [word] (if notverb word (str/replace word #"(maq|mek)$" ""))))
        (str/replace "ü" "u")
        (str/replace "ı" "i")
        (str/replace "ö" "o")
        (str/replace "â" "a")
        (str/replace "ş" "s")
        (str/replace "ğ" "g")
        (str/replace "ç" "c")
        (str/replace "ñ" "n")
        (str/replace "q" "k")
        )))
