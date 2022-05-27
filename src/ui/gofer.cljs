(ns ui.gofer
  (:require
   [cljs.core.async :as a]
   [cljs.tools.reader.edn :as edn]
   [sse]
   [taoensso.timbre :as log]
   [ui.time :as time]
   [ui.misc :as misc])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

::sse/keep

(defn available?
  "Is a piece of gofer data available in some form or another?"
  [state path]
  (get-in state [:gofer path :current :success?]))

(defn updated-at [state path]
  (when-let [completed-at (get-in state [:gofer path :current :completed-at])]
    (js/Date. completed-at)))

(defn stale?
  "Returns `true` if data is available, but past its expiry."
  [state path]
  (let [current (get-in state [:gofer path :current])]
    (and (:success? current)
         (when-let [expires (:expires-at current)]
           (time/before? (js/Date. expires) (time/now))))))

(defn cached?
  "Returns `true` if data is available, and it either has no expiry, or is still
  not past its expiry."
  [state path]
  (let [current (get-in state [:gofer path :current])]
    (and (:success? current)
         (if-let [expires (:expires-at current)]
           (time/before? (time/now) (js/Date. expires))
           true)
         (get-in state [:gofer path :data]))))

(defn failed?
  "Returns `true` if attempts at refreshing the current data has failed. `true`
  does not imply that there is no data (or the other way around), just that
  attempting to fetch it failed since last time we received anything."
  [state path]
  (->> (get-in state [:gofer path :log])
       (filter :completed-at)
       first
       :success?
       false?))

(defn loading?
  "Returns `true` if this piece of data is in the process of being loaded."
  [state path]
  (let [{:keys [requested-at completed-at]} (first (get-in state [:gofer path :log]))]
    (and requested-at (nil? completed-at))))

(defn get-availability-status
  "Returns a status for the piece of data, preferring `::available` if data is
  available in some form - even if it is currently loading, or attempts to
  reload it has failed. Returns one of

  `#{::available ::loading ::failed ::unknown}`"
  [state path]
  (cond
    (available? state path) ::available
    (loading? state path) ::loading
    (failed? state path) ::failed
    :default ::unknown))

(defn get-loading-status
  "Returns a status for the piece of data, preferring `::loading` if data is
  currently loading - even if previous data for this path is available. Returns
  one of

  `#{::available ::loading ::failed ::unknown}`"
  [state path]
  (cond
    (loading? state path) ::loading
    (available? state path) ::available
    (failed? state path) ::failed
    :default ::unknown))

(defn truncate-log [n log]
  (let [most-recent-success (first (filter :success? log))
        candidate (take n log)]
    (cond
      (not most-recent-success)
      candidate

      (some #{most-recent-success} candidate)
      candidate

      :else (concat (take (dec n) candidate)
                    [most-recent-success]))))

(defn add-log-entry [store path entry]
  (let [log (get-in @store [:gofer path :log])]
    (swap! store assoc-in [:gofer path :log] (truncate-log 3 (cons entry log)))))

(defn update-current-log-entry [store path entry]
  (let [log (get-in @store [:gofer path :log])
        data (:data entry)
        entry (merge (dissoc entry :data) (first log))]
    (swap! store (fn [state]
                   (cond-> (assoc-in state [:gofer path :log] (truncate-log 3 (cons entry (next log))))
                     (:success? entry) (assoc-in [:gofer path :current] entry)
                     data (assoc-in [:gofer path :data] data))))))

(defn complete-log-entry [now result]
  (cond-> {:completed-at now
           :success? (:success? result)}
    (:success? result) (assoc :data (:data result))
    (:ttl result) (assoc :expires-at (+ now (:ttl result)))
    (:meta result) (assoc :meta (:meta result))))

(defn complete-load [store {:keys [path] :as source} result]
  (log/info "Complete loading" source (dissoc result :data))
  (->> (complete-log-entry (time/timestamp) result)
       (update-current-log-entry store path)))

(defn request-data [store sources]
  (let [now (time/timestamp)]
    (doseq [{:keys [path]} sources]
      (add-log-entry store path {:requested-at now}))))

(defn evacuate-query-results [store sources]
  (for [source sources]
    (let [result {:success? false}]
      (complete-load store source result)
      {:result result
       :source source})))

(defn one-time-fn [f]
  (let [ff (memoize f)]
    (fn [] (ff))))

(defn query-data [store sources]
  (request-data store sources)
  (let [complete-ch (a/chan)]
    (try
      (let [state @store
            sse (js/SSE. (str (-> state :config :api-host) "/api/query")
                         (clj->js {:payload (pr-str {:requests (mapv #(select-keys % [:path]) sources)})
                                   :headers {"Authorization" (str "Bearer " (:token state))
                                             "Accept" "application/edn"
                                             "Content-Type" "application/edn"}
                                   :withCredentials true}))
            path->source (->> sources
                              (map (juxt :path identity))
                              (into {}))
            meta-atom (atom nil)
            remaining-atom (atom (set sources))]
        (.addEventListener sse "error"
                           (one-time-fn
                            (fn []
                              (let [status (.-status (.-xhr sse))]
                                (when-let [remaining-sources (seq @remaining-atom)]
                                  (go
                                    (doseq [{:keys [result source]}
                                            (do (js/console.log "Query failed with" status)
                                                (evacuate-query-results store remaining-sources))]
                                      (a/put! complete-ch {:path (:path source)
                                                           :success? (:success? result)}))
                                    (a/close! complete-ch)))))))
        (.addEventListener sse "message"
                           (fn [event]
                             (let [result (edn/read-string (.-data event))]
                               (if (:meta? result)
                                 (reset! meta-atom (dissoc result :meta?))
                                 (let [source (path->source (:path result))
                                       result (assoc result :meta @meta-atom)]
                                   (log/info "Loaded query result" (select-keys result [:path :success?]))
                                   (swap! remaining-atom disj source)
                                   (complete-load store source result)
                                   (a/put! complete-ch {:path (:path source)
                                                        :success? (:success? result)
                                                        :data (:data result)}))))))

        (.addEventListener sse "readystatechange"
                           (fn [event]
                             (when (and (= 2 (.-readyState event))
                                        (empty? @remaining-atom))
                               (a/close! complete-ch))))
        (.stream sse))
      (catch :default e
        (log/error e "Exception when setting up async querying")
        (go
          (doseq [source sources]
            (a/put! complete-ch {:path (:path source)
                                 :success? false}))
          (a/close! complete-ch))))
    complete-ch))

(defn reify-sources [state paths]
  (map (fn [path]
         {:path path
          :cached? (cached? state path)})
       paths))

(defn fetch-sources [store sources]
  (let [ch (a/chan (count sources))
        completed (atom [])
        state @store]
    (add-watch completed ::complete (fn [_ _ _ paths]
                                      (when (= (count sources) (count paths))
                                        (log/debug "Finished loading data, closing channel" paths)
                                        (a/close! ch))))
    (doseq [source (filter :cached? sources)]
      (log/info "Load data from the cache" source)
      (a/put! ch {:path (:path source)
                  :success? true
                  :cached? true
                  :data (get-in state [:gofer (:path source) :data])})
      (swap! completed conj (:path source)))
    (let [complete-ch (query-data store (remove :cached? sources))]
      (go
        (loop []
          (when-let [m (a/<! complete-ch)] ;; m is map with keys #{:path :success? :data}
            (a/>! ch m)
            (swap! completed conj (:path m))
            (recur)))))
    (when (= 0 (count sources))
      (log/debug "Nothing to load, closing channel" sources)
      (a/close! ch))
    ch))

(defn fetch [store paths]
  (try
    (fetch-sources store (reify-sources @store paths))
    (catch :default e
      (throw (ex-info "Exception when fetching" {:paths paths} e)))))

(defn refetch
  "Marks data as expired and fetches it over again"
  [store paths]
  (let [state (swap! store (fn [state]
                             (reduce
                              (fn [state source]
                                (cond-> state
                                  (get-in state [:gofer (:tuple source) :current])
                                  (assoc-in [:gofer (:tuple source) :current :expires-at] 0)))
                              state
                              (reify-sources state paths))))]
    (fetch-sources store (reify-sources state paths))))

(defn expire [store paths]
  (swap! store #(->> (mapcat (fn [path] [path [:gofer path]]) paths)
                     (apply misc/dissoc-in* %))))
