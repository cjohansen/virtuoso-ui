(ns ui.navigator
  (:require [ui.authentication :as auth]
            [ui.router :as router]
            [taoensso.timbre :as log]
            [ui.page :as page]))

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
        (page/get-default-page pages))
      (page/get-login-page pages)))

(defn preservable? [page]
  (not (:authentication-page? page)))

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

(defn go-to-location [element pages store location]
  (let [state @store
        target (resolve-target-location pages state location)]
    (when (nil? target)
      (log/error "Navigate has nowhere to go"
                 {:desired location
                  :target target
                  :authenticated? (auth/authenticated? state)
                  :login-page (page/get-login-page pages)
                  :default-page (page/get-default-page pages)}))
    (log/debug "Desired vs target" {:desired location
                                    :target target})
    (update-preserved-location! pages store location target)
    (log/info "Updating URL" {:location target})
    (router/update-url (:config state) pages (:current-location state) target)
    (swap! store assoc :current-location target)))
