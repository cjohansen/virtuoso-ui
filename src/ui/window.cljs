(ns ui.window
  (:require [ui.misc :refer [debounce]]
            [taoensso.timbre :as log]))

(defn set-window-size [store]
  (let [dim {:w js/window.innerWidth
             :h js/window.innerHeight}]
    (log/info "Set window size" dim)
    (swap! store assoc :size dim)))

(def ^:private set-window-size-debounced (debounce set-window-size 100))

(defn keep-size-up-to-date [store]
  (set-window-size store)
  (set! js/window.onresize #(set-window-size-debounced store)))
