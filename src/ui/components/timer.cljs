(ns ui.components.timer
  (:require [dumdom.core :as d]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(d/defcomponent timer
  :on-render (fn [el]
               (let [separator (.querySelector el ".timer-separator")]
                 (go
                   (set! separator.style.transition "opacity 400ms")
                   (<! (timeout 400))
                   (set! separator.style.opacity 0.5)
                   (set! separator.style.transition "opacity 100ms")
                   (<! (timeout 500))
                   (set! separator.style.opacity 1))))
  [{:keys [minutes seconds progress]}]
  [:div.timer
   [:div.timer-time
    [:span.timer-minutes minutes]
    [:span.timer-separator ":"]
    [:span.timer-seconds seconds]]
   [:div.timer-progress-track
    [:div.timer-progress-line {:style {:width (str (* progress 100) "%")}}]]])
