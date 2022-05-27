(ns ^:figwheel-hooks virtuoso.dev
  (:require [dumdom.component]
            [gadget.inspector :as inspector]
            [ui.app :as app]
            [ui.event-bus :as bus]
            [virtuoso.i18n :as i18n]
            [virtuoso.core :as virtuoso])
  (:require-macros [ui.config.dev :as config]))

(defonce store (atom (app/initialize-store (config/load-config))))
(defonce event-bus (bus/create-event-bus))
(defonce pages (atom (virtuoso/get-pages-map)))
(defonce dictionaries (atom (i18n/init-dictionaries)))

(defn get-inspector-data [page-data]
  (if (= #{:page-data :layout-data} (-> page-data keys set))
    page-data
    {:page-data page-data}))

(defn render-component [component data]
  (let [{:keys [layout-data page-data]} (get-inspector-data data)]
    (inspector/inspect "Layout data" (or layout-data {}))
    (inspector/inspect "Page data" page-data))
  (component data))

(defonce started
  (do
    (set! dumdom.component/*render-comments?* true)
    (set! dumdom.component/*render-eagerly?* true)
    (set! *print-namespace-maps* false)
    (inspector/inspect "Store" store)
    (inspector/inspect "Dictionaries" dictionaries)
    (add-watch store ::dev (fn [_ _ _ state]
                             (when-let [location (:current-location state)]
                               (inspector/inspect "Location" location))))
    (virtuoso/bootup
     {:store store
      :dictionaries dictionaries
      :element (js/document.getElementById "app")
      :pages pages
      :event-bus event-bus
      :render-component #'render-component})))

(defn ^:after-load refresh []
  (reset! dictionaries (i18n/init-dictionaries))
  (reset! pages (virtuoso/get-pages-map))
  (swap! store assoc ::reloaded-at (.getTime (js/Date.))))
