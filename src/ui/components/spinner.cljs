(ns ui.components.spinner)

(defn spinner [& [attrs]]
  [:svg.spinner (merge attrs {:viewBox "0 0 100 100"})
   [:defs
    [:linearGradient {:id "gradient" :x1 "0%" :y1 "0%" :x2 "0%" :y2 "100%"}
     [:stop {:offset "0%" :stop-color "var(--spinner-line-1)"}]
     [:stop {:offset "38%" :stop-color "var(--spinner-line-2)"}]
     [:stop {:offset "100%" :stop-color "var(--spinner-line-3)"}]]]
   [:g.spinner-inner
    [:circle {:fill "none" :stroke-linecap "round" :stroke-width "10" :cx "50" :cy "50" :r "31" :stroke "var(--spinner-track)"}]
    [:circle.spinner-circle {:r "31" :stroke "url(#gradient)" :fill "none" :stroke-dashoffset "0" :stroke-width "10" :cx "50" :cy "50" :stroke-dasharray "200" :stroke-linecap "round"}]]])
