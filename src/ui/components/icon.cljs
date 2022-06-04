(ns ui.components.icon
  (:require [ui.components.svg :as svg]))

(defn icon [id & [options]]
  (svg/svg (assoc options
                  :id id
                  :view-box "0 0 48 48")))
