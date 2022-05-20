(ns ui.page
  (:require [clojure.string :as str]
            [dumdom.core :as d]
            [taoensso.timbre :as log]
            [ui.i18n :as i18n]
            [ui.misc :as misc]
            [ui.time :as time]))

(defn format-page-title [state title]
  (->> [title (get-in state [:config :site-title])]
       (remove empty?)
       (str/join " - ")))

(defn get-title [dictionaries state page]
  (let [{:keys [page-title page-title-fn]} page
        title (cond
                page-title page-title
                page-title-fn (page-title-fn state)
                :default "")]
    (if (keyword? title)
      (i18n/lookup dictionaries (:locale state) title)
      title)))

(defn set-title! [dictionaries state page]
  (->> (get-title dictionaries state page)
       (format-page-title state)
       (set! js/document.title)))

(defn get-page [pages location]
  (when-let [page (get pages (:location/page-id location))]
    (merge
     {:component (constantly [:h1 (pr-str (:location/page-id location)) " has no component"])
      :prepare (fn [state location] state)}
     page)))

(defn get-required-data [state location page]
  (or (:required-data page)
      (when-let [f (:required-data-fn page)]
        (f state location))))

(defn prepare-state [state location page]
  (cond-> (assoc state :now (time/now))
    (map? (:gofer assoc))
    (update :gofer #(apply select-keys % (get-required-data state location page)))))

(defn render-location [{:keys [pages dictionaries render-component element]} state location]
  (if-let [{:keys [prepare component] :as page} (get-page @pages location)]
    (let [render-fn (or render-component (fn [component data] (component data)))]
      (set-title! @dictionaries state page)
      (d/render
       (->> (prepare (prepare-state state location page) location)
            (i18n/tr @dictionaries (:locale state))
            (render-fn component))
       element))
    (log/error "Cannot render location, no page: " {:location location})))

(def request-render
  (misc/debounce
   (fn [& args]
     (js/requestAnimationFrame
      #(apply render-location args)))
   30))

(defn render-current-location [app state]
  (request-render app state (:current-location state)))
