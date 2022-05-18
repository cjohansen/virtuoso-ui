(ns ui.components.timer
  (:require [dumdom.core :as d]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(d/defcomponent timer
  :on-render (fn [el {:keys [progress]}]
               (let [line (.querySelector el ".timer-progress-line")]
                 (if (= 0 progress)
                   ;; The progress line should only animate as it "runs out". If
                   ;; it's reset (to any value > 0), it should just reset
                   ;; instantly.
                   (set! line.style.transition "none")
                   (when (re-find #"^none" line.style.transition)
                     (go
                       ;; Reset transition asynchronously to avoid the line
                       ;; animating slowly from 0 to 100% width.
                       (<! (timeout 100))
                       (set! line.style.transition "width 1s linear")))))
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
