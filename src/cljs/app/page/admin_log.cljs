(ns app.page.admin_log
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [app.state :as s]
            [app.multi :refer [page-contents]]
            [lib.ajax :refer [GET POST PUT]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))


(rf/reg-event-db
 :get-missed-words
 (fn [db _]
   (go
    (let [res (<! (GET s/conf "/missed-words"))
          status (:status res)
          content (:content res)]
      (when (= 200 status)
        (rf/dispatch [:process-missed-words content])
      ))
    )
   db))

(rf/reg-event-db
 :process-missed-words
 (fn [db [_ words]]
   (assoc db :missed-words words)))




(defn Admin []
  [:p "admin"])



(defmethod page-contents :admin-log []
  (fn []
    [:table {:class "table table-striped table-bordered table-condensed table-hover"}
     [:thead
      [:tr
       [:th "Searched"]
       [:th "Found"]]]
     [:tbody
      (doall (map (fn [row] [:tr
                             [:td (first row)]
                             [:td (second row)]])
           @(rf/subscribe [:get-missed-words])))
      ]]
    ))
