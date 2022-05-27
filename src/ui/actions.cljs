(ns ui.actions
  (:require [ui.event-bus :as bus]
            [ui.misc :as misc]))

(defn register-actions [store event-bus]
  (let [subs (partial bus/subscribe event-bus ::action)]
    (subs :action/assoc-in #(apply swap! store misc/assoc-in* %))
    (subs :action/dissoc-in #(apply swap! store misc/dissoc-in* %))))
