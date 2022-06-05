(ns ui.navigator
  (:require [cljs.core.async :refer [<!]]
            [taoensso.timbre :as log]
            [ui.authentication :as auth]
            [ui.gofer :as gofer]
            [ui.misc :as misc]
            [ui.page :as page]
            [ui.router :as router])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn location-reachable? [pages state location]
  (let [page (pages (:location/page-id location))]
    (and (:location/page-id page)
         (if (auth/authenticated? state)
           (and (not (:authentication-page? page))
                (not (:login-page? page)))
           (not (:requires-authentication? page true))))))

(defn resolve-target-location [pages state location]
  (or (when (location-reachable? pages state (:preserved-location state))
        (:preserved-location state))
      (when (location-reachable? pages state location)
        location)
      (when (auth/authenticated? state)
        (page/as-location (page/get-default-page pages)))
      (page/as-location (page/get-login-page pages))))

(defn preservable? [page]
  (and (not (:authentication-page? page))
       (not (:login-page? page))))

(defn update-preserved-location! [pages store desired target]
  (when (and (= target desired)
             (:preserved-location @store))
    (log/info "Clearing preserved location" (:preserved-location @store))
    (swap! store dissoc :preserved-location))
  (when (and (not= target desired)
             (preservable? (pages (:location/page-id desired))))
    (log/info "Preserving location" desired)
    (swap! store assoc :preserved-location desired))
  nil)

(defn notify-page-on-data [page state location message]
  (try
    (when-let [f (:on-required-data page)]
      (f state location message))
    (catch :default e
      (log/error e "Failed to notify page about new data"
                 {:location location
                  :page page
                  :message message}))))

(defn fetch-data-for-location [{:keys [store authenticator] :as app} page location]
  (let [f (or (:required-data-fn page)
              (constantly (:required-data page)))]
    (go
      (<! (auth/<ensure-authenticated! authenticator app))
      (when (:token @store)
        (loop [fetched #{}]
          (let [required-data (remove fetched (f @store location))]
            (when (seq required-data)
              (let [ch (gofer/fetch store required-data)]
                (recur
                 (into
                  fetched
                  (loop [paths []]
                    (if-let [message (<! ch)]
                      (do
                        (when (:success? message)
                          (notify-page-on-data page @store location message))
                        (recur (conj paths (:path message))))
                      paths))))))))))))

(defn go-to-location [{:keys [pages store] :as app} location]
  (let [state @store
        pages @pages
        target (resolve-target-location pages state location)]
    (when (nil? target)
      (log/error "Navigate has nowhere to go"
                 {:desired location
                  :target target
                  :authenticated? (auth/authenticated? state)
                  :login-page (page/get-login-page pages)
                  :default-page (page/get-default-page pages)}))
    (update-preserved-location! pages store location target)
    (log/info "Updating URL" {:location target
                              :current-location (:current-location state)})
    (router/update-url (:config state) pages (:current-location state) target)
    (swap! store (fn [state]
                   (-> state
                       (misc/dissoc-in* [:transient (:current-location state)])
                       (assoc :current-location target))))
    (fetch-data-for-location app (get pages (:location/page-id target)) target)))
