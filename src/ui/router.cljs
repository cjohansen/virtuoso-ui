(ns ui.router
  "Parse URLs, query params and match them against routes"
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]))

(defn parse-qs-val [v]
  (let [v (js/decodeURIComponent v)]
    (cond
      (re-find #"^\d+$" v) (js/parseInt v 10)
      (re-find #"^\d+\.\d+$" v) (js/parseFloat v)
      (= "true" v) true
      (= "false" v) false
      :else v)))

(defn parse-query-params
  "Parse a query string into a map with keyword keys. Query params that have no
  value (e.g. `...&key&other-key`) will be parsed with `true` as the value."
  [query-string]
  (some->> (str/split query-string #"&")
           (remove empty?)
           seq
           (map (fn [s]
                  (if (re-find #"=" s)
                    (let [[k & v] (str/split s #"=")]
                      [(keyword k) (parse-qs-val (str/join "=" v))])
                    [(keyword s) true])))
           (into {})))

(defn- blank? [v]
  (or (nil? v)
      (and (coll? v) (empty? v))
      (= v "")))

(defn encode-query-params
  "Encode a map as a query string. Empty values (nil, empty strings, empty
  collections, false values) are omitted from the resulting string."
  [params]
  (if (empty? params)
    ""
    (->> params
         (remove (comp blank? second))
         (map (fn [[k v]]
                (if (true? v)
                  (name k)
                  (str (name k) "=" (js/encodeURIComponent v)))))
         (str/join "&"))))

(defn parse-url-to-route
  "Parses a URL string, optionally with a query string, to a map suitable for
  matching against routes."
  [url]
  (let [[path query] (str/split url #"\?")]
    {:location/route-data (drop 1 (str/split path #"/"))
     :location/query-params (parse-query-params query)}))

(defn matches-route?
  "Tests if a concrete `parsed` URL matches a `location` containing a
  `:location/route`. Returns `true` for matching a location."
  [location parsed]
  (when (= (count (:location/route location))
           (count (:location/route-data parsed)))
    (->> (:location/route-data parsed)
         (map vector (:location/route location))
         (every? (fn [[pattern v]]
                   (or (keyword? pattern) (= pattern v)))))))

(defn reify-location
  "Given a `parsed` URL and a matching `location`, returns a map of
  `:location/page-id`, `:location/params` (data from keyword parts of the route,
  if applicable), and `:location/query-params` (if any)."
  [location parsed]
  (let [params (->> (:location/route-data parsed)
                    (map vector (:location/route location))
                    (filter (comp keyword? first))
                    (into {}))]
    (cond-> {:location/page-id (:location/page-id location)}
      (not-empty params)
      (assoc :location/params params)

      (not-empty (:location/query-params parsed))
      (assoc :location/query-params (:location/query-params parsed)))))

(defn resolve-route
  "Given a map of `{location/page-id location}` and a `url` string, returns a
  reified location (see `reifiy-location`)."
  [pages url]
  (loop [pages (seq (vals pages))
         parsed (parse-url-to-route url)]
    (or
     (when-let [location (first pages)]
       (when (matches-route? location parsed)
         (reify-location location parsed)))
     (when pages
       (recur (next pages) parsed)))))

(defn url-to
  "Given a map of `{location/page-id location}` and a location map, returns a URL
  string."
  [pages location]
  (let [qs (encode-query-params (:location/query-params location))
        url (->> (get-in pages [(:location/page-id location) :location/route])
                 (map (fn [p]
                        (if (keyword? p)
                          (get-in location [:location/params p] "[]")
                          p)))
                 (into [""])
                 (str/join "/"))]
    (cond-> url
      (empty? url) (str "/")
      (not-empty qs) (str "?" qs))))

(defn get-current-browser-url
  "Get the current browser URL. Strips off the hash if using `:hash-urls?`"
  [config]
  (if (:hash-urls? config)
    (let [hash (.-hash js/location)]
      (when (seq hash) (subs hash 1)))
    (str (.-pathname js/location) (.-search js/location))))

(defn get-url
  "Like `url-to`, but respects the `:hash-urls?` config key."
  [config pages location]
  (cond->> (url-to pages location)
    (:hash-urls? config)
    (str "#")))

(defn update-url
  "Update the browser URL, respecting the `:hash-urls?` config key. Does nothing
  if the target URL is the same as the current URL. Uses replaceState when the
  target shares page-id with the current location."
  [config pages curr target]
  (let [url (get-url config pages target)]
    (when-not (= url (get-current-browser-url config))
      (try
        (if (= (:location/page-id target) (:location/page-id curr))
          (.replaceState js/history false false url)
          (.pushState js/history false false url))
        (catch :default e
          (log/warn "Failed to ui.router/update-url" url e))))))

(defn get-current-location
  "Route the current browser URL and return the resulting location. If no match is
  found, returns a location with the `:location/page-id` `:ui.router/not-found`."
  [config pages]
  (let [url (or (get-current-browser-url config) "/")]
    (or (resolve-route pages url)
        {:location/page-id ::not-found
         :location/url url})))

