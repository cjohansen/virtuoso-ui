(ns virtuoso.dev-server
  (:require [clojure.java.io :as io]))

(defn record-regression-result [req]
  (let [{:keys [file contents]} (read-string (slurp (:body req)))]
    (spit file contents)
    {:status 200}))

(defn handler [req]
  (if (= "/api/regression" (:uri req))
    (record-regression-result req)
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (slurp (io/resource "public/index.html"))}))
