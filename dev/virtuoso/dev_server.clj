(ns virtuoso.dev-server
  (:require [clojure.java.io :as io]))

(defn handler [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (slurp (io/resource "public/index.html"))})
