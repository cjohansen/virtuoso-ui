(ns ui.event-bus
  (:require [taoensso.timbre :as log]))

(defn publish [subscribers topic & args]
  (when topic
    (try
      (doseq [subscriber @subscribers]
        (cond
          (= topic (::topic subscriber))
          (apply (::handler subscriber) args)

          (nil? (::topic subscriber))
          (apply (::handler subscriber) topic args)))
      (catch :default e
        (log/warn e "Error when publishing to topic" topic args)
        (throw e)))))

(defn remove-subscriber [subscribers subscriber]
  (->> subscribers
       (remove #(= (select-keys % [::topic ::name])
                   (select-keys subscriber [::topic ::name])))))

(defn add-subscriber [subscribers subscriber]
  (-> subscribers
      (remove-subscriber subscriber)
      (conj subscriber)))

(defn subscribe
  ([subscribers name handler]
   (subscribe subscribers name nil handler))
  ([subscribers name topic handler]
   (swap! subscribers add-subscriber
          (cond-> {::name name ::handler handler}
            (not-empty topic) (assoc ::topic topic)))))

(defn unsubscribe [subscribers name & [topic]]
  (swap! subscribers remove-subscriber
         (cond-> {::name name}
           (not-empty topic) (assoc ::topic topic))))

(defn publish-actions [subscribers actions & xargs]
  (doseq [[topic & args] actions]
    (apply publish subscribers topic (concat args xargs))))

(defn create-event-bus []
  (atom []))
