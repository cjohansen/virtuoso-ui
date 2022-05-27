(ns ui.logger
  (:require [cljs.pprint :as pprint]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(defn configure-logging []
  (log/merge-config!
   {:min-level :debug
    :appenders
    {:console
     {:enabled? true
      :min-level :debug
      :output-fn (fn [data]
                   (str
                    (->> (concat
                          [(second (str/split (.toISOString (:instant data)) #"T"))
                           (str/upper-case (name (:level data)))
                           (str "[" (:?ns-str data) ":" (:?line data) "]")
                           "-"]
                          (take-while string? (:vargs data)))
                         (str/join " "))
                    (when-let [args (->> (:vargs data)
                                         (drop-while string?)
                                         (map (fn [d]
                                                (with-out-str (pprint/pprint d))))
                                         seq)]
                      (str "\n" (str/join "\n" args)))))
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
