(ns ui.svg
  (:require [clojure.string :as str]))

(defn id-str [id]
  (-> (str (namespace id) "-" (name id))
      (str/replace #"\." "-")))

(defn get-url [id]
  (-> (str "/" (namespace id) "/" (name id))
      (str/replace #"\." "/")
      (str ".svg")))
