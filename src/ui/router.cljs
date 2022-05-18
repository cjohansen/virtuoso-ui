(ns ui.router
  "Parse URLs, query params and match them against routes"
  (:require [clojure.string :as str]))

(defn parse-query-params
  "Parse a query string into a map with keyword keys. Query params that have no
  value (e.g. `...&key&other-key`) will be parsed with `true` as the value."
  [query-string]
  (->> (str/split query-string #"&")
       (map (fn [s]
              (if (re-find #"=" s)
                (let [[k & v] (str/split s #"=")]
                  [(keyword k) (js/decodeURIComponent (str/join "=" v))])
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
  (let [qs (encode-query-params (:location/query-params location))]
    (cond-> (->> (get-in pages [(:location/page-id location) :location/route])
                 (map (fn [p]
                        (if (keyword? p)
                          (get-in location [:location/params p] "[]")
                          p)))
                 (into [""])
                 (str/join "/"))
      (not-empty qs) (str "?" qs))))