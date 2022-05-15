(ns ui.components.spinner-cards
  (:require [ui.components.spinner :refer [spinner]]
            [dumdom.devcards :refer-macros [defcard]]))

(defcard spinner-example
  [:div (spinner)])
