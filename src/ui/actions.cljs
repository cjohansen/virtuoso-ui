(ns ui.actions
  (:require [ui.event-bus :as bus]
            [ui.misc :as misc]
            [ui.navigator :as navigator]
            [ui.picard :as picard]))

(defn register-actions [{:keys [store event-bus] :as app}]
  (let [subs (partial bus/subscribe event-bus ::action)]
    (subs :actions/assoc-in #(apply swap! store misc/assoc-in* %&))
    (subs :actions/dissoc-in #(apply swap! store misc/dissoc-in* %&))
    (subs :actions/command #(picard/<command! store event-bus %&))
    (subs :actions/go-to-location #(apply navigator/go-to-location app %&))))
