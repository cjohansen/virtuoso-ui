(ns ui.freezer
  "Into the freezer goes important data so that we can quickly show the user a UI
  when restarting the app."
  (:require [ui.misc :refer [debounce throttle]]
            [cljs.tools.reader.edn :as edn]
            [taoensso.timbre :as log]))

(defn dissoc-many [m ks]
  (apply dissoc m ks))

(defn prepare-for-freezing [{:keys [exclude-ks include-ks]} state]
  (cond-> state
    (seq exclude-ks) (dissoc-many exclude-ks)
    (seq include-ks) (select-keys include-ks)))

(defn log-freeze [state]
  (log/debug "Freeze keys" (keys state))
  state)

(defn freeze [state opt]
  (when (and (:token state) (not (:freezer/disabled? state)))
    (->> state
         (prepare-for-freezing opt)
         log-freeze
         pr-str
         (js/localStorage.setItem "app-state"))))

(defn thaw []
  (let [s (js/localStorage.getItem "app-state")]
    (try
      (edn/read-string s)
      (catch :default e
        (log/error e "Failed to thaw freezer" s)))))

(defn keep-up-to-date [store & [opt]]
  (let [calm-freeze (throttle (debounce freeze 1000) 5000)]
    (add-watch store ::freezer (fn [_ _ _ new-state]
                                 (calm-freeze new-state opt)))))
