(ns virtuoso.test-helper
  (:require [cljs.core.async :refer [<!]]
            [clojure.test :refer [is]]
            [flare.diff :as diff]
            [flare.report :as report]
            [ui.http :as http]
            [ui.page :as page]
            [virtuoso.i18n :as i18n]
            [ui.event-bus :as bus])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [ui.test-helper :refer [load-previous-result]]))

(defn try-scenario [page state location & [locale]]
  (page/prepare-page-data
   (assoc state :locale (or locale :en))
   location
   (i18n/init-dictionaries)
   page))

(defn run-scenario [dicts id page state location]
  (for [locale (keys dicts)]
    (let [recorded (load-previous-result id locale)]
      {:id id
       :locale locale
       :recorded-result recorded
       :computed-result (page/prepare-page-data
                         (assoc state :locale locale)
                         location
                         dicts
                         page)})))

(defn run-regression-test
  ([id page state location]
   (run-regression-test (i18n/init-dictionaries) id page state location))
  ([dicts id page state location]
   ;; This function could have been simplified by calling `run-scenario`, but we
   ;; want the `is` macro to include the call to page/prepare-page-data in its
   ;; output when it fails
   (doall
    (for [locale (keys dicts)]
      (let [state (assoc state :locale locale)]
        (is (= (page/prepare-page-data state location dicts page)
               (load-previous-result id locale))))))))

(defn run-regression-tests
  ([scenarios]
   (run-regression-tests (i18n/init-dictionaries) scenarios))
  ([dicts scenarios]
   (doall
    (for [[id page state location] scenarios]
      (run-regression-test dicts id page state location)))))

(defn diagnose-scenario
  ([scenario]
   (diagnose-scenario (i18n/init-dictionaries) scenario))
  ([dicts [id page state location]]
   (when id
     (for [res (run-scenario dicts id page state location)]
       (let [diff (diff/diff (:recorded-result res) (:computed-result res))]
         (cond-> res
           diff (assoc :diffs (report/report diff))))))))

(defn update-recorded-result [[id page state location]]
  (go
    (doseq [res (run-scenario (i18n/init-dictionaries) id page state location)]
      (<! (http/request
           {:method :post
            :url "/api/regression"
            :edn-params {:file (str "dev-resources/regression/"
                                    (namespace id) "/" (name id) "-" (name (:locale res)) ".edn")
                         :contents (:computed-result res)}})))))

(defn find-failing-scenarios [scenarios]
  (let [dicts (i18n/init-dictionaries)]
    (->> scenarios
         (filter #(some false? (apply run-regression-test dicts %))))))

(defn inspect-failure-diffs [scenarios]
  (let [dicts (i18n/init-dictionaries)]
    (->> scenarios
         (mapcat #(diagnose-scenario dicts %))
         (filter :diffs)
         (map (juxt :id :locale :diffs)))))

(defn diagnose-first-failure [scenarios]
  (->> scenarios
       find-failing-scenarios
       first
       diagnose-scenario))

(defn update-first-failing [scenarios]
  (->> scenarios
       find-failing-scenarios
       first
       update-recorded-result))

(defn update-all-failing [scenarios]
  (->> scenarios
       find-failing-scenarios
       (map update-recorded-result)
       doall))

(defn create-test-app []
  (let [event-bus (bus/create-event-bus)
        log (atom [])]
    (bus/subscribe event-bus ::logger #(swap! log conj %&))
    {:event-bus event-bus
     :store (atom {})
     :event-log log}))
