(ns ui.app
  (:require [clojure.walk :as walk]
            [dumdom.core :as d]
            [taoensso.timbre :as log]
            [ui.actions :as actions]
            [ui.event-bus :as bus]
            [ui.logger :as logger]
            [ui.navigator :as navigator]
            [ui.page :as page]
            [ui.router :as router]
            [ui.session :as session]
            [ui.window :as win]))

(defn initialize-store [config]
  {:config config
   :locale (:default-locale config :en)})

(defn- a-element [el]
  (loop [el el]
    (cond
      (nil? el) nil
      (and (.-href el)
           (= "A" (.-tagName el))) el
      :default (recur (.-parentNode el)))))

(defn- get-path [href]
  (when (not-empty href)
    (.replace href js/location.origin "")))

(defn relay-body-clicks [app e]
  (let [path (some->> (.-target e) a-element .-href get-path)]
    (when-let [location (some->> path (router/resolve-route @(:pages app)))]
      (.preventDefault e)
      (if (or e.ctrlKey e.metaKey)
        (.open js/window path "_blank")
        (navigator/go-to-location app location)))))

(defn go-to-current-location [app]
  (->> (router/get-current-location (:config @(:store app)) @(:pages app))
       (navigator/go-to-location app)))

(defn handle-actions [store event-bus e actions]
  (.preventDefault e)
  (let [target-val (some-> e .-target .-value)]
    (->> actions
         (walk/prewalk
          #(case %
             :event/target.value target-val
             %))
         (bus/publish-actions event-bus))))

(defn log-action [topic & args]
  (let [[a b c d e f g h i j] args]
    (case (count args)
      10 (log/debug :ui.logger/one-line topic a b c d e f g h i j)
      9 (log/debug :ui.logger/one-line topic a b c d e f g h i)
      8 (log/debug :ui.logger/one-line topic a b c d e f g h)
      7 (log/debug :ui.logger/one-line topic a b c d e f g)
      6 (log/debug :ui.logger/one-line topic a b c d e f)
      5 (log/debug :ui.logger/one-line topic a b c d e)
      4 (log/debug :ui.logger/one-line topic a b c d)
      3 (log/debug :ui.logger/one-line topic a b c)
      2 (log/debug :ui.logger/one-line topic a b)
      1 (log/debug :ui.logger/one-line topic a)
      0 (log/debug :ui.logger/one-line topic)
      (log/debug :ui.logger/one-line topic args))))

(defn main
  "main supports the bootup process. It performs all the bootup tasks that can
  safely be repeated (and that needs to be repeated when code changes). This
  function can be safely called from a reload hook in your development setup."
  [app]
  (actions/register-actions app)
  (doseq [register-actions (keep :register-actions (vals @(:pages app)))]
    (register-actions app)))

(defn bootup
  "Perform one-time configuration of app resources and add listeners in
  appropriate places to get the app running"
  [{:keys [store event-bus] :as app}]
  (let [config (:config @store)]
    (logger/configure-logging)
    (log/info "Starting app with config" config)

    (swap! store assoc
           ::bootup-at (.getTime (js/Date.))
           :session (session/get-session config))

    ;; handle window.onerror

    (when (:log-event-bus-messages? config)
      (bus/subscribe event-bus ::app log-action))

    (d/set-event-handler! #(apply handle-actions store event-bus %&))

    (set! js/window.onpopstate (fn [] (go-to-current-location app)))

    (js/document.body.addEventListener "click" #(relay-body-clicks app %))

    (win/keep-size-up-to-date store)

    (add-watch store ::render (fn [_ _ _ state]
                                (page/render-current-location app state)))

    (main app)

    (go-to-current-location app)))
