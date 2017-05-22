(ns app.queries
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :route
 (fn [db _]
   (:route db)))


(rf/reg-sub
 :get-current-word
 (fn [db _]
   (:current-word db)))


(rf/reg-sub
 :get-missed-words
 (fn [db _]
   (:missed-words db)))
