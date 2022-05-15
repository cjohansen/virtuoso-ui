(ns ^:figwheel-hooks ui.devcards
  (:require [devcards.core :as devcards]
            [ui.components.spinner-cards]))

(enable-console-print!)

(defn render []
  (devcards/start-devcard-ui!))

(defn ^:after-load render-on-relaod []
  (render))

(render)
