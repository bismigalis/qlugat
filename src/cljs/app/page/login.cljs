(ns app.page.login
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs.core.async :as a :refer [<!]]
            [accountant.core :as accountant]
            [app.multi :refer [page-contents]]
            [app.state :as s]

            [lib.ajax :refer [GET POST PUT]]
            [lib.log :refer [log]])

    (:require-macros [reagent.ratom :refer [reaction]]
                     [cljs.core.async.macros :refer [go go-loop alt!]])
)

(defn valid-credentials? [data]
  true)


(defmethod page-contents :login []
  (let [data (r/atom {:username "" :password ""})
        username (r/cursor data [:username])
        password (r/cursor data [:password])
        loading (r/atom false)
        error (r/atom "")
        ]
    (fn []
      [:div {:style {:position :relative
                     :width "300px"}}
       ;;(if @loading [LoadingWrap])
       (if-not (empty? @error) [:p {:class "bg-danger"}  @error])

       [:label "Username: "
        [:input {:type :text
                 :value @username
                 :on-change #(reset! username (.-target.value %))}]]

       [:br]

       [:label "Password: "
        [:input {:type :password
                 :value @password
                 :on-change #(reset! password (.-target.value %))}]]

       [:br]

       [:button {:class "btn"
                 :on-click (fn [e]
                             (when (valid-credentials? @data)
                               (reset! loading true)
                               (go
                                 (let [res (<! (POST s/conf "/login" @data))]
                                   (reset! loading false)
                                   (case (:status res)
                                     200 (do (reset! s/AUTH-TOKEN (get (:content res) "token"))
                                             ;;(.setToken history "")
                                             #_(rf/dispatch [:set-route {:current-page :index
                                                                       ;;:route-params route-params
                                                                         }])
                                             (accountant/navigate! "/")
                                             )
                                     401 (reset! error (get (:content res) "error")))
                                   ))))}
        "Submit"]
       ;;[:pre (str @data)]
       ])
    ))
