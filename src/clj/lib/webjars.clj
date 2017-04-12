(ns lib.webjars
  (:import [org.webjars WebJarAssetLocator])
  (:require [ring.middleware.head :as head]
            [ring.middleware.resource :as resource]
            [ring.util.codec :as codec]
            [ring.util.request :as req]
            [ring.util.response :as resp]
            [io.pedestal.interceptor.helpers :as h]
            [io.pedestal.interceptor.chain :as c]
            [clojure.tools.logging :as log]))

(def ^:private webjars-pattern
  #"META-INF/resources/webjars/([^/]+)/([^/]+)/(.*)")

(defn- asset-path [prefix resource]
  (let [[_ name version path] (re-matches webjars-pattern resource)]
    (str prefix "/" name "/" path)))

(defn- asset-map [^WebJarAssetLocator locator prefix]
  (->> (.listAssets locator "")
       (map (juxt (partial asset-path prefix) identity))
       (into {})))

(defn- request-path [request]
  (codec/url-decode (req/path-info request)))

#_(defn wrap-webjars
  ([handler]
     (wrap-webjars handler "/assets"))
  ([handler prefix]
     (let [assets (asset-map (WebJarAssetLocator.) prefix)]
       (fn [request]
         (if (#{:head :get} (:request-method request))
           (if-let [path (assets (request-path request))]
             (-> (resp/resource-response path)
                 (head/head-response request))
             (handler request))
(handler request))))))
(defn make-resource-handler []
  (let [assets (asset-map (WebJarAssetLocator.) "/assets")]
    (h/before (fn [context]
                (let [request (:request context)]
                  (if (#{:head :get} (:request-method request))
                    (if-let [path (assets (request-path request))]
                      (-> context
                          (assoc :response (-> (resp/resource-response path)
                                               (head/head-response request)))
                        )
                      context)
                    context)
                  )
                ))
    ))
