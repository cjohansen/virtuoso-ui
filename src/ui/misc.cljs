(ns ui.misc
  (:require [cljs.core.async :refer [<! alts! chan put! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn debounce
  "Creates a new function that will call function `f` some `ms` milliseconds
  after the last invocation, discarding arguments of all invocations in that
  timeframe but the last."
  [f ms]
  (let [c (chan)]
    (go
      (loop [args (<! c)]
        (let [[value port] (alts! [c (timeout ms)])]
          (if (= port c)
            (recur value)
            (do ;; or timed out
              (apply f args)
              (recur (<! c)))))))
    (fn [& args]
      (put! c (or args [])))))

(defn assoc-in* [m & args]
  (assert (= 0 (mod (count args) 2)) "assoc-in* takes a map and pairs of path value")
  (assert (->> args (partition 2) (map first) (every? vector?)) "each path should be a vector")
  (->> (partition 2 args)
       (reduce (fn [m [path v]]
                 (assoc-in m path v)) m)))

(defn dissoc-in* [m & args]
  (reduce (fn [m path]
            (cond
              (= 0 (count path)) m
              (= 1 (count path)) (dissoc m (first path))
              :else (let [[k & ks] (reverse path)]
                      (update-in m (reverse ks) dissoc k))))
          m args))
