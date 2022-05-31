(ns virtuoso.components.brain-cards
  (:require [dumdom.devcards :refer-macros [defcard]]
            [virtuoso.components.brain :as brain]))

(defcard default-brain
  [:div {}
   (brain/brain
    {:id "default-brain"})])

(defcard themed-brain
  [:div {}
   (brain/brain
    {:theme {:text ["#22e1ff"
                    "#20b7ef"
                    "#1ea0e7"
                    "#1d8fe1"
                    "#3181d3"
                    "#4077c9"
                    "#5369bb"
                    "#625eb1"]
             :gradient ["#22e1ff" "#1d8fe1" "#625eb1"]}
     :id "virtuoso-brain"})])

(defcard brain-only
  [:div {}
   (brain/brain
    {:text? false
     :id "brain-only"})])

(defcard white-brain
  [:div {}
   (brain/brain
    {:text? false
     :theme {:gradient ["#ffffff"]}
     :id "white-brain"})])
