(ns ui.config.dev
  (:require [clojure.java.io :as io]))

(def config (read-string (slurp (io/resource "dev.edn"))))

(defmacro load-config []
  `~config)
