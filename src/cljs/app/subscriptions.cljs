(ns app.subscriptions
  (:require [re-frame.core :as rf]))


(rf/reg-event-db
  :initialize
  (fn [_ _]
    {:route ""
     :suggest-db {}
     :list []
     :pos -1
     :current-word ""
     :dict-entry {}
     :modal false
     }))

(rf/reg-event-db
 :set-current-word
 (fn [db [_ word]]
   (assoc db :current-word word)))
