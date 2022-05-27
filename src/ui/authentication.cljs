(ns ui.authentication)

(defn authenticated? [state]
  (not (nil? (:token state))))
