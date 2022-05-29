(ns ui.actions
  (:require [cljs.core.async :refer [<!]]
            [ui.authentication :as auth]
            [ui.event-bus :as bus]
            [ui.misc :as misc]
            [ui.navigator :as navigator]
            [ui.picard :as picard])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn command! [{:keys [store authenticator event-bus] :as app} opt command]
  (go
    (<! (auth/<ensure-authenticated! authenticator app))
    (picard/<command! store event-bus opt command)))

(defn register-actions [{:keys [store event-bus] :as app}]
  (let [subs (partial bus/subscribe event-bus ::action)]
    (subs :actions/assoc-in #(apply swap! store misc/assoc-in* %&))
    (subs :actions/dissoc-in #(apply swap! store misc/dissoc-in* %&))
    (subs :actions/command #(command! app {:include-token? true} %&))
    (subs :actions/public-command #(picard/<command! store event-bus {} %&))
    (subs :actions/go-to-location #(apply navigator/go-to-location app %&))))
