(ns ui.logger
  (:require [taoensso.timbre :as log]))

(defn configure-logging []
  (log/merge-config!
   {:min-level :debug
    :appenders
    {:console
     {:enabled? true
      :min-level :debug
      :fn #(if (:level %)
             (let [output-fn (:output-fn %)
                   formatted (output-fn %)
                   level (:level %)]
               (condp = level
                 :warn (js/console.warn formatted)
                 :fatal (js/console.error formatted)
                 :error (js/console.error formatted)
                 (js/console.info formatted)))
             (try
               (log/error "Log message has no level" %)
               (catch :default e
                 (js/console.error "Fatal logging error: failed to log missing log level"))))}}}))
