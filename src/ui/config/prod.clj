(ns ui.config.prod
  (:require [clojure.java.io :as io]))

(def git-sha (get (System/getenv) "GIT_SHA"))

(def config (cond-> (read-string (slurp (io/resource "prod.edn")))
              git-sha (assoc :git-sha git-sha)
              :always (assoc :build-date (str (java.time.Instant/now)))))

(defmacro load-config []
  `~config)
