(ns ui.components.button-cards
  (:require [dumdom.devcards :refer-macros [defcard]]
            [ui.components.button :refer [button]]))

(defcard button
  (button {:text "Click it"
           :href "/lul"}))

(defcard button-disabled
  (button {:text "Click it"
           :href "/lul"
           :disabled? true}))

(defcard button-spinner
  (button {:text "Log in"
           :href "/lul"
           :disabled? true
           :spinner? true}))
