(ns app.frontend
  (:require [app.common :as c]
            [app.dictionary :as d]
            [app.stemmer :refer [get-stem detect-lang]]

            [reagent.core :as r]
            [cljs.core.async :as a]
            [clojure.string :as str]

            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [goog.json :as json])

  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go go-loop alt!]])

  (:import goog.net.XhrIo
           ;;goog.History
           goog.history.Html5History))

(def HOST "http://qlugat.my")

(defn get-token []
  (str js/window.location.pathname js/window.location.search))

(defonce ROUTE (r/atom ""))
(defn handle-url-change [e]
  ;; log the event object to console for inspection
  (reset! ROUTE (.-token e))
  )

(defonce history (doto (Html5History.)
                   (.setUseFragment false)
                   (goog.events/listen HistoryEventType/NAVIGATE
                                       #(handle-url-change %))
                   (.setEnabled true)))
(defonce SUGGEST-DB (atom {}))
(defonce LIST (r/atom []))
(defonce POS (r/atom -1))
(defonce CURRENT-WORD (atom ""))
(defonce DICT-ENTRY (r/atom {}))
(defonce LOADING (r/atom false))
(defonce MODAL (r/atom false))
(defonce AUTH-TOKEN (atom false))

(def WORDS-CH (a/chan (a/dropping-buffer 1)))
(def CMD-CH (a/chan))


(defn log [x]
  (js/console.log (clj->js x)))

(defn AJAX [url method content]
  (def XXX content)
  (let [complete (atom false)
        ch (a/chan 1)]
    (go
      (<! (a/timeout 400))
      (if-not @complete (reset! LOADING true)))

    (XhrIo.send url
                (fn [event]
                  (reset! LOADING false)
                  (reset! complete true)
                  (let [resp (-> event .-target .getResponseJson)
                        headers (-> event .-target .getResponseHeaders)
                        status-code (-> event .-target .getStatus)
                        status-text (-> event .-target .getStatusText)]
                    (go (a/>! ch {:status status-code
                                  :message status-text
                                  :headers headers
                                  :content (if resp (js->clj resp) {})
                                  })
                        (a/close! ch))))
                method
                (json/serialize (clj->js content))
                #js {"Content-Type" "application/json"
                     "Token" @AUTH-TOKEN})
    ch))

(defn GET [url]
  (AJAX url "GET" nil))

(defn PUT [url content]
  (AJAX url "PUT" content))

(defn POST [url content]
  (AJAX url "POST" content))


#_(defn get-root [word]
  (-> word
      (str/replace "'" "")
      (str/replace #" I+$" "")
      (str/replace #"\|.+" "")
      )
  )

#_(defn get-display [word]
  (-> word
      (str/replace "|" "")
      (str/replace "'" "\u0301")
      ))

#_(defn preprocess-article [article root]
  (if article
    (-> article
        (str/replace ";" "<br>")
        (str/replace "~" root)
        (str/replace "◊" "<br>◊<br>")
        )
    "")
  )

(defn update-wordlist
  "Given word, updates wordlist with entries that
  starts with word"
  [stem len]
  (let [f (if (<= len 4)
            ;;(fn [x] (filter #(<= (count %) 4) x))
            (partial take 16)
            identity)
        l (vec (f (d/get-list-by-token @SUGGEST-DB stem)))]
    (reset! POS (if (empty? l) -1 0))
    (reset! DICT-ENTRY {})
    (reset! LIST l)
    ))

#_(defn reset-wordlist []
  (reset! POS -1)
  (reset! LIST [])
  (reset! ARTICLE {})
  )

(defn update-suggest-db [letter]
  (go
    (let [res (<! (GET (str HOST "/suggest?token=" letter)))
          status (:status res)
          content (:content res)]
      (when (= 200 status)
        (swap! SUGGEST-DB d/set-list-by-letter letter content))
      )))

(defn get-word-by-pos [pos]
  (get-in @LIST [pos :word]))

(defn get-article [word]
  (go (if (count word)
        (let [res (<! (GET (str HOST "/get_json?word=" word)))
              status (:status res)
              content (:content res)]
          (case status
            200 (do (reset! DICT-ENTRY content)
                    (reset! CURRENT-WORD word)
                    )
            404 (do (reset! DICT-ENTRY {})
                    (reset! CURRENT-WORD "")
                    #_(.focus (.getElementById js/document "search-input")))
            )
          (.focus (.getElementById js/document "search-input"))
          ))))

;; update wordlist widget when edit word in input widget
(go-loop []
  (let [w (a/<! WORDS-CH)
        s (get-stem w)
        len (count s)
        letter (first s)]
    (if (and (= 1 len)
             (not (get @SUGGEST-DB letter)))
      (a/<! (update-suggest-db letter)))
    (update-wordlist s len)
    (reset! CURRENT-WORD w)
    )
  (recur))

#_(go-loop []
  (let [c (a/<! CMD-CH)]
    (case (:type c)
      :keydown
      )
    (recur)))


(defn on-form-submit [e]
  (.preventDefault e)
  (let  [word e.target.elements.word.value]

    ))

(defn display-word [word accentPos]
  (if accentPos
    (str (subs word 0 (dec accentPos))
         "\u0301"
         (subs word (dec accentPos)))
    word))

(defn display-article [word article shorteningPos]
  ;;(log [word article shorteningPos])
  (-> article
      (str/replace "~" (if shorteningPos
                         (subs word 0 shorteningPos)
                         word))
      (str/replace #"(</b>)" "$1 ")
      (str/replace "; " "<br>")
      (str/replace "\n" "<br>")
      (str/replace "◊" "<br>◊<br>")
      ))

;;
;; AJAX
;;
(defn put-word [article]
  (go
    (let [res (<! (PUT (str HOST "/edit_word") @article))]
      (if (= (:status res) 200)
        (do
          ;;(cljs.pprint/pprint (:content res))
          ;;(log (get @word "word"))
          (get-article @CURRENT-WORD)
          (reset! article false)
          ))

      )))

;;
;; UI
;;

(defn SuggestList [words cur-word]
  [:ul {:class "word-list"
        :style {:margin "0 1ex 0 0"
                :padding 0
                :background-color "#f7f7f7"
                :border "1px solid #a9a9a9"
                :list-style-type :none
                :font-size "14px"
                :min-height "40ex"
                :width "20ex"
                }
        }
   (if-not (empty? words)
     (doall (map-indexed (fn [i x]
                           (let [active (= i @POS)
                                 style {:padding "0 .5ex"}]
                             [:li {:style (if active
                                            (merge style {:color :white :background-color "#000080"})
                                            style)
                                   :key (:word x)
                                   ;;:value (:word x)
                                   :on-click (fn []
                                               (reset! POS i)
                                               (reset! cur-word (:word x))
                                               (get-article (:word x))
                                               )
                                   }
                              (:word x)]))
                         words)))
   ])

(defn EditButton [article]
  [:button {:class "btn btn-default btn-xs"
            :on-click #(reset! MODAL article)
            }
   [:span {:class "glyphicon glyphicon-edit"}]
   ])

(defn SubmitButton []
  [:button {:class "btn btn-primary"
            :on-click #(get-article @CURRENT-WORD)}
   [:span {:class "glyphicon glyphicon-search"}]
   ])

(defn ClearButton []
  [:button {:class "btn btn-primary"
            :on-click #(log %)}
   [:span {:class "glyphicon glyphicon-remove"}]
   ])

(defn Article [{:strs [word shorteningPos] :as w} {:strs [article accentPos] :as a} edit]
  [:dl
   [:dt
    [:span (display-word word accentPos)] " "
    (if (and edit @AUTH-TOKEN) [EditButton a])]
   [:dd {:style {:margin-left "1ex"}
         :dangerouslySetInnerHTML {:__html (display-article word article shorteningPos)}}]])

(defn DictEntry [dict-entry]
  [:pre {:style {:min-width "50em" :margin-top "1em" :white-space :nowrap}}
   (for [article (get dict-entry "articles")]
     ^{:key (get article "id")} [Article dict-entry article true])
   ]
  )

(defn Loading []
  [:div "Loading..."]
  )

(defn Modal [article]
  [:div
   [:div {:class "modal-backdrop fade in"}]
   [:div {:class "modal" :style {:display :block}}
    [:div {:class "modal-dialog"}
     [:div {:class "modal-content"}
      [:div {:class "modal-header"}
       [:button {:on-click #(reset! article false) :class "close"} [:span "×"]]
       [:h4 {:class "modal-title"} "Edit word"]
       ]
      [:div {:class "modal-body"}
       [:div {:class "form-group"}
        #_[:label "Word"]
        #_[:input {:default-value #_(get @word "word") :class "form-control"
                 :on-input #_(swap! word assoc "word" (.-target.value %))}]
        ]
       [:div {:class "form-group"}
        [:label "Article"]
        [:textarea {:default-value (get @article "article") :rows 6 :class "form-control"
                    :style {:font-family :monospace}
                    :on-input #(swap! article assoc "article" (.-target.value %))
                    }]
        ]
       #_[Article @article]
       ]
      [:div {:class "modal-footer"}
       [:button {:class "btn btn-primary"
                 :on-click #(put-word article)}
        "Save changes"]
      ]]]]]
  )

(defn InputWord [cur-word]
  [:input
       {:type :search
        :auto-focus true
        :id "search-input"
        :class :form-control
        :value @cur-word
        :on-change #(do (reset! cur-word (.-target.value %))
                        (a/put! WORDS-CH (.-target.value %)))
        :on-key-down (fn [e]
                        (case (.-keyCode e)
                          13 (get-article (if (> @POS -1) (get-word-by-pos @POS) @CURRENT-WORD))
                          38 (if (> @POS 0) (swap! POS dec))
                          40 (if (< (inc @POS) (count @LIST)) (swap! POS inc))
                          ""
                          )
                        )}])

(defn Link [url title]
  [:a {:href url
       :on-click (fn [e]
                   (.preventDefault e)
                   (.setToken history url))}
   title]
  )


(defn LoadingWrap []
  [:div {:style {:position :absolute
                 :top 0
                 :left 0
                 :background "#fffc"
                 :width "100%"
                 :height "100%"
                 :z-index 100
                 }}]
  )


(defn valid-credentials? [data]
  true)

(defn LoginForm []
  (let [data (r/atom {:username "" :password ""})
        username (r/cursor data [:username])
        password (r/cursor data [:password])
        loading (r/atom false)
        error (r/atom "")
        ]
    (fn []
      [:div {:style {:position :relative
                     :width "300px"}}
       (if @loading [LoadingWrap])
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
                                 (let [res (<! (POST (str HOST "/login") @data))]
                                   (reset! loading false)
                                   (case (:status res)
                                     200 (do (reset! AUTH-TOKEN (get (:content res) "token"))
                                             (.setToken history ""))
                                     401 (reset! error (get (:content res) "error")))
                                   ))))}
        "Submit"]
       [:pre (str @data)]])
    ))


(defn Dictionary []
  (let [cur-word (r/atom "")]
    (fn []
      [:div
       [:table
        [:tbody
         [:tr {:style {:vertical-align :top}}
          [:td [SuggestList @LIST cur-word]]
          [:td {:class "form-inline"}
           [:div {:class "input-group"}
            ;;[:span {:class "input-group-btn"} [ClearButton]]
            [InputWord cur-word]
            [:span {:class "input-group-btn"} [SubmitButton]]]
           (if @LOADING
             [Loading]
             (if-not (empty? @DICT-ENTRY) [DictEntry @DICT-ENTRY]))
           ]
          ]
         #_[:tr {:style {:vertical-align :top}}
            [:td ]
            [:td ]
            ]
         ]
        ]
       ;;[:div (clj->js @AUTH-TOKEN)]
       (if @MODAL [Modal MODAL])
       ]
      )))


(defn App []
  [:div
   ;;[Link "" "Home"]
   ;;[Link "login" "Login"]
   (case @ROUTE
     "" [Dictionary]
     "login" [LoginForm]
  )])

(defn main []
  ;;(hook-browser-navigation!)
  (r/render-component [App]
                      (js/document.getElementById "container"))
  ;;(events/removeAll js/document)
  ;;(events/listen js/document goog.events.EventType.KEYDOWN keydown-handler)
  )

(main)