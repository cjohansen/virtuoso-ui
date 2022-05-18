(ns ui.components.timer-cards
  (:require [ui.components.timer :refer [timer]]
            [dumdom.devcards :refer-macros [defcard]]))

(defcard timer-example
  (timer
   {:minutes "04"
    :seconds "53"
    :progress 0.97}))

(defcard timer-less-progress
  (timer
   {:minutes "03"
    :seconds "17"
    :progress 0.6566}))

(defn pad [n]
  (str (when (< n 10) "0") n))

(defcard timer-live-timer
  (fn [store]
    (js/setTimeout
     (fn []
       (swap! store update :ms #(if (= % 0) 300 (dec %))))
     1000)
    (let [ms (:ms @store)]
      (timer
       {:minutes (pad (int (/ ms 60)))
        :seconds (pad (int (mod ms 60)))
        :progress (/ ms 300.0)})))
  {:ms 300})

(defcard timer-live-timer-shorter-time
  (fn [store]
    (js/setTimeout
     (fn []
       (swap! store update :ms #(if (= % 0) 60 (dec %))))
     1000)
    (let [ms (:ms @store)]
      (timer
       {:minutes (pad (int (/ ms 60)))
        :seconds (pad (int (mod ms 60)))
        :progress (/ ms 60.0)})))
  {:ms 60})
