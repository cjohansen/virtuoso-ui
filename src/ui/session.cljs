(ns ui.session
  (:require [cljs.tools.reader.edn :as edn]
            [taoensso.timbre :as log]
            [ui.time :as time]))

(defn load-session []
  (when-let [s (js/sessionStorage.getItem "session")]
    (try
      (edn/read-string s)
      (catch :default e
        (log/warn "Failed to read-string session data" {:session-data s})
        nil))))

(defn create-session [config]
  (cond-> {:id (random-uuid)
           :started-at (time/timestamp)}
    (:git-sha config) (assoc :git-sha (:git-sha config))))

(defn get-session [config]
  (or (load-session)
      (create-session config)))
