(ns virtuoso.core
  (:require [ui.app :as app]))

(defn get-pages-map [pages]
  (->> pages
       (map (juxt :location/page-id identity))
       (into {})))

(defn bootup [app]
  (app/bootup app))
