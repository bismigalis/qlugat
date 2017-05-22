(ns app.state
  (:require [reagent.core :as r]
            [cljs.core.async :as a]
))

(defonce SUGGEST-DB (atom {}))
(defonce LIST (r/atom []))
(defonce POS (r/atom -1))
(defonce DICT-ENTRY (r/atom {}))
(defonce OTHER-DICT-ENTRY (r/atom {}))
(defonce MODAL (r/atom false))
(defonce AUTH-TOKEN (atom false))
(defonce LOADING (r/atom false))
(def conf {:auth-token AUTH-TOKEN
           :loading LOADING})


;;(def WORDS-CH (a/chan (a/dropping-buffer 1)))
(def WORDS-CH (a/chan))
(def CMD-CH (a/chan))
