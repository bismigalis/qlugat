(ns app.app-test
  (:require [cljs.test :refer-macros [deftest is async]]
            [app.dictionary :as d]))

(deftest sanity-check
  (let [wl (d/make-wordlist)
        x (d/set-list-by-letter wl \a ["a" "ab"])]

    (is (= ["ab"]
           (d/get-list-by-token x "ab")))))
