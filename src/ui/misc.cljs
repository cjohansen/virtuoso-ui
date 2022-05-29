(ns ui.misc
  (:require [cljs.core.async :refer [<! alts! chan put! timeout]]
            [ui.event-bus :as bus])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn throttle
  "Creates a new function that will call function `f` only once every `ms`
  milliseconds. The very first invocation is done immediately. After that, all
  subsequent calls but the last are discarded. The last call is then performed
  once `ms` has passed."
  [f ms]
  (let [args-ch (chan)]
    (go
      (loop [args (<! args-ch)
             hold-ch nil]
        (if (and args (not hold-ch))
          (do (apply f args)
              (recur nil (timeout ms)))
          (let [[value port] (alts! (if hold-ch
                                      [args-ch hold-ch]
                                      [args-ch]))]
            (if (= port args-ch)
              (recur value hold-ch) ;; new arguments
              (recur args nil)))))) ;; timed out
    (fn [& args]
      (put! args-ch (or args [])))))

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

(defn assoc-in*
  "Takes a map and pairs of path value to assoc-in to the map. Makes `assoc-in`
  work like `assoc`, e.g.:

  ```clj
  (assoc-in* {}
             [:person :name] \"Christian\"
             [:person :language] \"Clojure\")
  ;;=>
  {:person {:name \"Christian\"
            :language \"Clojure\"}}
  ```"
  [m & args]
  (assert (= 0 (mod (count args) 2)) "assoc-in* takes a map and pairs of path value")
  (assert (->> args (partition 2) (map first) (every? vector?)) "each path should be a vector")
  (->> (partition 2 args)
       (reduce (fn [m [path v]]
                 (assoc-in m path v)) m)))

(defn dissoc-in*
  "Takes a map and paths to dissoc from it. An example explains it best:

  ```clj
  (dissoc-in* {:person {:name \"Christian\"
                        :language \"Clojure\"}}
              [:person :language])
  ;;=>
  {:person {:name \"Christian\"}}
  ```

  Optionally pass additional paths.
  "
  [m & args]
  (reduce (fn [m path]
            (cond
              (= 0 (count path)) m
              (= 1 (count path)) (dissoc m (first path))
              :else (let [[k & ks] (reverse path)]
                      (update-in m (reverse ks) dissoc k))))
          m args))

(defn swapper
  "Returns a function that when called passes all its arguments to `f` and then
  performs a `swap!` on `ref` with `assoc-in*` and the return value from the
  call to `f`."
  [ref f]
  (fn [& args]
    (swap! ref #(apply assoc-in* % (apply f % args)))))

(defn event-emitter
  "Returns a function that when called passes all its arguments to `f` and then
  publishes the returned actions on the provided `event-bus`."
  [event-bus f]
  (fn [& args]
    (bus/publish-actions event-bus (apply f args))))
