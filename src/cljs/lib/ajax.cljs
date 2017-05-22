(ns lib.ajax
  (:require [goog.json :as json]
            [cljs.core.async :as a])
  (:import goog.net.XhrIo)
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
)


(defn AJAX [conf url http-method content]
  ;;(def XXX content)
  (let [loading (:loading conf)
        auth-token (:auth-token conf)
        complete (atom false)
        ch (a/chan 1)]
    (go
      (<! (a/timeout 500))
      (if-not @complete (reset! loading true)))

    (XhrIo.send url
                (fn [event]
                  (reset! loading false)
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
                http-method
                (json/serialize (clj->js content))
                #js {"Content-Type" "application/json"
                     "Token" @auth-token})
    ch))

(defn GET [conf url]
  (AJAX conf url "GET" nil))

(defn PUT [conf url content]
  (AJAX conf url "PUT" content))

(defn POST [conf url content]
  (AJAX conf url "POST" content))
