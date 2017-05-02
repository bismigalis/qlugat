(ns app.queries
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :get-current-word
 (fn [db _]
   (:current-word db)))
