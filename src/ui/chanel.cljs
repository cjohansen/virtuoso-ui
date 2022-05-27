(ns ui.chanel
  (:require [cljs.core.async :as a]))

(defonce chans (atom []))

(defn get-chans-report []
  (vec
   (for [[id buf _ inst] @chans]
     [id (count buf) inst])))

(defn full? [chan-id]
  (some (fn [[id buf _ _]]
          (and (= id chan-id)
               (= 32 (count buf))))
        @chans))

(defn remove-chan [ch]
  (swap! chans #(vec (remove (fn [[_ _ c _]] (= ch c)) %))))

(defn close! [ch]
  (a/close! ch)
  (remove-chan ch))

(defn chan
  ([id] (chan id nil nil nil))
  ([id buf-or-n] (chan id buf-or-n nil nil))
  ([id buf-or-n xform] (chan id buf-or-n xform nil))
  ([id buf-or-n xform ex-handler]
   (let [buf-or-n (if (= buf-or-n 0) nil buf-or-n)
         buf (cond
               (nil? buf-or-n) (a/buffer 32)
               (number? buf-or-n) (a/buffer buf-or-n)
               :else buf-or-n)
         ch (a/chan buf xform ex-handler)]
     (swap! chans conj [id buf ch (js/Date.)])
     ch)))
