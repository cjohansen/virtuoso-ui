(ns virtuoso.core
  (:require [ui.app :as app]
            [virtuoso.home-page.page :as home-page]
            [virtuoso.login-page.page :as login-page]))

(defn get-pages-map []
  (->> [home-page/page
        login-page/page]
       (map (juxt :location/page-id identity))
       (into {})))

(defn bootup [app]
  (app/bootup app))
