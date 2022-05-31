(ns ^:figwheel-hooks ui.devcards
  (:require [devcards.core :as devcards]
            [ui.components.button-cards]
            [ui.components.input-cards]
            [ui.components.spinner-cards]
            [ui.components.timer-cards]
            [virtuoso.components.brain-cards]))

(enable-console-print!)

(defn render []
  (devcards/start-devcard-ui!))

(defn ^:after-load render-on-relaod []
  (render))

(render)
