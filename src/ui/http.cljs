(ns ui.http
  (:require [cljs.core.async :refer [<! put!]]
            [cljs.tools.reader.edn :as edn]
            [ui.chanel :as chanel]))

(defn get-fetch-params [req]
  (cond-> (dissoc req :token :url :edn-params)
    (:token req) (assoc-in [:headers "Authorization"] (str "Bearer " (:token req)))
    (:token req) (assoc :credentials "same-origin")
    (:edn-params req) (assoc-in [:headers "Content-Type"] "application/edn")
    (:edn-params req) (assoc :body (pr-str (:edn-params req)))))

(defn parse-response-body [res body]
  (or
   (when (re-find #"\bedn\b" (.get (.-headers res) "content-type"))
     (try
       (edn/read-string body)
       (catch :default e
         nil)))
   body))

(defn request [req]
  (let [ch (chanel/chan ::fetch)]
    (-> (js/fetch (:url req) (clj->js (get-fetch-params req)))
        (.then
         (fn [res]
           (-> (.text res)
               (.then
                (fn [body]
                  (put! ch {:status (.-status res)
                            :headers (->> (.-headers res)
                                          .keys
                                          (map (fn [k]
                                                 [k (.get (.-headers res) k)]))
                                          (into {}))
                            :body (parse-response-body res body)})
                  (chanel/close! ch)))))))
    ch))
