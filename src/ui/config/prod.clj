(ns ui.config.prod
  (:require [clojure.java.io :as io]))

(def config (read-string (slurp (io/resource "prod.edn"))))

(defmacro load-config []
  `~config)
