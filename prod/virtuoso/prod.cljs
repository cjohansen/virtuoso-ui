(ns virtuoso.prod
  (:require [ui.event-bus :as bus]
            [virtuoso.i18n :as i18n]
            [virtuoso.core :as virtuoso])
  (:require-macros [ui.config.dev :as config]))

(defonce started
  (virtuoso/bootup
   {:store (atom {:config (config/load-config)})
    :dictionaries (atom (i18n/init-dictionaries))
    :element (js/document.getElementById "app")
    :pages (atom (virtuoso/get-pages-map))
    :event-bus (bus/create-event-bus)}))
