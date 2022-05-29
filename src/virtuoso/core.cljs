(ns virtuoso.core
  (:require [cljs.core.async :refer [<!]]
            [taoensso.timbre :as log]
            [ui.app :as app]
            [ui.authentication :as auth]
            [ui.picard :as picard]
            [ui.time :as time]
            [virtuoso.home-page.page :as home-page]
            [virtuoso.login-page.page :as login-page])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def token-expiry-buffer (* 30 1000))

(defrecord TokenAuthenticator []
  auth/Authenticator

  (<ensure-authenticated! [_ {:keys [store event-bus]}]
    (go
      (let [{:keys [token token-info]} @store]
        (when (auth/token-expired? token-info (+ (time/timestamp) token-expiry-buffer))
          (log/info "Token expired, attempting refresh")
          (<! (picard/<command! store event-bus {} [:login/refresh-token token])))))))

(defn get-pages-map []
  (->> [home-page/page
        login-page/page]
       (map (juxt :location/page-id identity))
       (into {})))

(defn bootup [app]
  (app/bootup app))
