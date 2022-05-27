(ns ui.logger
  (:require [cljs.pprint :as pprint]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(defn long-map? [m]
  (and (map? m)
       (< 2 (count (keys m)))))

(defn split-log-vargs [vargs]
  (let [one-line? (and (some #{:ui.logger/one-line} vargs)
                       (not (some long-map? vargs)))
        vargs (remove #{:ui.logger/one-line} vargs)
        ss (take-while string? vargs)]
    (if (seq ss)
      {:messages ss
       :data-args (drop-while string? vargs)
       :section-separator "\n"
       :data-separator (if one-line? " " "\n")}
      {:messages (take-while keyword? vargs)
       :data-args (drop-while keyword? vargs)
       :section-separator (if one-line? " " "\n")
       :data-separator (if one-line? " " "\n")})))

(defn configure-logging []
  (log/merge-config!
   {:min-level :debug
    :appenders
    {:console
     {:enabled? true
      :min-level :debug
      :output-fn (fn [data]
                   (let [{:keys [messages data-args section-separator data-separator]}
                         (split-log-vargs (:vargs data))]
                     (str
                      (->> (concat
                            [(second (str/split (.toISOString (:instant data)) #"T"))
                             (str/upper-case (name (:level data)))
                             (str "[" (:?ns-str data) ":" (:?line data) "]")]
                            messages)
                           (str/join " "))
                      (when-let [args (->> data-args
                                           (map (fn [d]
                                                  (str/trim (with-out-str (pprint/pprint d)))))
                                           seq)]
                        (str section-separator (str/join data-separator args))))))
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
