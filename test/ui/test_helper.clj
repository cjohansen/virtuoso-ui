(ns ui.test-helper
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn get-test-name [file]
  (let [[_ scenario locale]
        (re-find #"(.*)\-([^-]+)$"
                 (-> (.getName file)
                     (str/split #"\.edn$")
                     first))]
    [(keyword (.getName (.getParentFile file)) scenario) (keyword locale)]))

(def regressions
  (->> (io/file (io/resource "regression"))
       file-seq
       (filter (fn [file]
                 (re-find #"\.edn$" (.getPath file))))
       (map (fn [file]
              [(get-test-name file) (read-string (slurp file))]))
       (into {})))

(defmacro load-previous-result [id locale]
  `(get (quote ~regressions) [~id ~locale]))

