(ns ui.navigator)

(defn go-to-location [store element location]
  (swap! store assoc :current-location location))
