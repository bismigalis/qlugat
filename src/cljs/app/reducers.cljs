(ns app.reducers
  (:require [re-frame.core :as rf]
            [lib.log :refer [log]]
            [app.state :as s]
            [cljs.core.async :as a :refer [<!]]
            [lib.ajax :refer [GET POST PUT]]
            [accountant.core :as accountant]
  )
(:require-macros [reagent.ratom :refer [reaction]]
                 [cljs.core.async.macros :refer [go go-loop alt!]]))


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
 :set-route
 (fn [db [_ route]]
   (assoc db :route route)
   ))



(rf/reg-event-db
 :set-current-word
 (fn [db [_ word]]
   (assoc db :current-word word)))



(rf/reg-event-db
 :load/index
 (fn [db _]
   db))

(rf/reg-event-db
 :set-missed-words
 (fn [db [_ words]]
   (assoc db :missed-words (get words "words"))))


(rf/reg-event-db
 :load/admin-log
 (fn [db _]
  (go
    (let [{:keys [status content]} (<! (GET s/conf "/missed-words"))]
      (when (= 200 status)
        (rf/dispatch [:set-missed-words content]))
      ))
   db))
