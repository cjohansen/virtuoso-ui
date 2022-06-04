(ns ui.components.svg
  (:require [dumdom.core :as d]
            [ui.svg :as svg]))

(defn use-svg [el target]
  (set! (.-innerHTML el)
        (str "<use xlink:href=\"" target "\"/>")))

(d/defcomponent svg
  :keyfn :id
  :on-render (fn [el {:keys [id]}]
               (when-not (.-firstChild el)
                 (let [str-id (svg/id-str id)]
                   (if (not (nil? (js/document.getElementById str-id)))
                     (use-svg el (str "#" str-id))
                     (use-svg el (str (svg/get-url id) "#root"))))))
  [options]
  [:svg.svg (cond-> (dissoc options :id :size)
              (:size options) (update :style merge {:width (:size options)
                                                    :headers (:size options)}))])
