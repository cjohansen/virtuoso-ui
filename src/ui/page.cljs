(ns ui.page
  (:require [clojure.string :as str]
            [dumdom.core :as d]
            [taoensso.timbre :as log]
            [ui.i18n :as i18n]
            [ui.misc :as misc]
            [ui.time :as time]))

(defn as-location [page]
  (select-keys page [:location/page-id
                     :location/query-params
                     :location/params]))

(defn get-login-page [pages]
  (first (filter :login-page? (vals pages))))

(defn get-default-page [pages]
  (first (filter :default-page? (vals pages))))

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

(defn prepare-page-data [state location dicts page]
  (when-let [prepare (:prepare page)]
    (->> (prepare (prepare-state state location page) location)
         (i18n/tr dicts (:locale state)))))

(defn render-location [{:keys [pages dictionaries render-component element]} state location]
  (if-let [page (get-page @pages location)]
    (let [render-fn (or render-component (fn [component data] (component data)))
          dicts @dictionaries]
      (set-title! dicts state page)
      (d/render
       (->> (prepare-page-data state location dicts page)
            (render-fn (:component page)))
       element))
    (log/error "Cannot render location, no page: " {:location location})))

(def request-render
  (misc/debounce
   (fn [& args]
     (js/requestAnimationFrame
      #(apply render-location args)))
   30))

(defn render-current-location [app state]
  (when-let [location (:current-location state)]
    (request-render app state location)))
