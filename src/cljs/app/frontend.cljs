(ns app.frontend
  (:require
   [bidi.bidi :as bidi]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [accountant.core :as accountant]

   [app.multi :refer [page-contents]]
   [app.page.index]
   [app.page.login]
   [app.page.admin_log]
   [app.state :as s]

   [lib.log :refer [log]]
   )
  (:import goog.net.Cookies))


(defn get-token []
  (str js/window.location.pathname js/window.location.search))


(defmethod page-contents :four-o-four []
  "Non-existing routes go here"
  [:span
   [:h1 "404"]
   [:p "What you are looking for, "]
   [:p "I do not have."]])


(defmethod page-contents :default []
  "Configured routes, missing an implementation, go here"
  [:span
   [:h1 "404: My bad"]
   [:pre.verse
    "This page should be here, but I never created it."]])

(def app-routes
  ["/" [
        ["" :index]
        ["login" :login]
        ["admin" [
                  ["/log" :admin-log]]
         ]
        ["missing-route" :missing-route]
        [true :four-o-four]
        ]])


(defn Link [route title cur-route]
  [:li {:class (if (= route cur-route) "active" "")}
   [:a {:href (bidi/path-for app-routes route)} title]])

(defn App []
  (fn []
    (let [cur-route (:current-page @(rf/subscribe [:route]))]
      [:div
       (if @s/AUTH-TOKEN
         [:ul {:class "nav nav-tabs"}
          [Link :index "Home" cur-route]
          [Link :admin-log "Log" cur-route]
          ]
         )

       [(page-contents cur-route)]
       #_[:pre (js/JSON.stringify (clj->js @(rf/subscribe [:route])))]
       ])))

(defn main []
  (reset! s/AUTH-TOKEN (.get (Cookies. js/document) "auth-token"))
  (accountant/configure-navigation!
   {:nav-handler (fn [path] ;;(1)
                   (let [match (bidi/match-route app-routes path) ;;(2)
                         current-page (:handler match) ;;(3)
                         route-params (:route-params match)] ;;(4)

                     (rf/dispatch [(keyword (str "load/" (name current-page)))])
                     (rf/dispatch-sync [:set-route {:current-page current-page :route-params route-params}])
                     ))
    :path-exists? (fn [path] (boolean (bidi/match-route app-routes path)))})

  (rf/dispatch-sync [:initialize])
  (accountant/dispatch-current!)
  (r/render-component [App] (. js/document (getElementById "container")))
  )

#_(defn main []
  ;;(hook-browser-navigation!)
  (r/render-component [App]
                      (js/document.getElementById "container"))
  ;;(events/removeAll js/document)
  ;;(events/listen js/document goog.events.EventType.KEYDOWN keydown-handler)
  )



(main)
