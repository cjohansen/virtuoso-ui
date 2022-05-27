(ns virtuoso.components.logos
  (:require [dumdom.core :as d]
            [clojure.string :as str]))

(def viewboxes
  {:virtuoso/gradient "114 0 490 200"
   :virtuoso/flat "114 0 490 200"
   :virtuoso/name "0 224 718.67 135"})

(d/defcomponent gradient-logo
  :keyfn :url
  :on-render (fn [el {:keys [url]}]
               (set! (.-innerHTML el)
                     (str "<use xlink:href=\"" url "#icon\"/>")))
  [options]
  [:svg {:view-box (viewboxes (:logo/kind options))}])

(d/defcomponent flat-logo
  :keyfn (juxt :url :colors :color)
  :on-render (fn [el {:keys [url color colors]}]
               (set! (.-innerHTML el)
                     (str "<use xlink:href=\"" url "#icon\""
                          "style=\""
                          "--left-bg: " (or color (:left colors)) ";"
                          "--right-bg: " (or color (:right colors)) ";"
                          "--notes-bg: " (or color (:notes colors)) ";"
                          "\"/>")))
  [options]
  [:svg {:view-box (viewboxes (:logo/kind options))}])

(d/defcomponent name-logo
  :keyfn (juxt :url :color :colors)
  :on-render (fn [el {:keys [url color colors]}]
               (let [colors (vec colors)]
                 (set! (.-innerHTML el)
                       (str "<use xlink:href=\"" url "#icon\" style=\""
                            (->> (for [i (range 8)]
                                   (str "--color" (inc i) ": " (or (get colors i) color)))
                                 (str/join "; "))
                            "\"/>"))))
  [options]
  [:svg {:view-box (viewboxes (:logo/kind options))}])

(def themes
  {:theme/blue-gradient
   {:colors ["#22e1ff"
             "#20b7ef"
             "#1ea0e7"
             "#1d8fe1"
             "#3181d3"
             "#4077c9"
             "#5369bb"
             "#625eb1"]}})

(defn resolve-theme [data]
  (merge data (get themes (:theme data))))

(defn render [data]
  (case (:logo/kind data)
    :virtuoso/gradient (gradient-logo data)
    :virtuoso/flat (flat-logo data)
    :virtuoso/name (-> data
                       resolve-theme
                       name-logo)))
