(ns ui.app
  (:require
   [taoensso.timbre :as log]
   [ui.event-bus :as bus]
   [ui.logger :as logger]
   [ui.navigator :as navigator]
   [ui.page :as page]
   [ui.router :as router]
   [ui.window :as win]))

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

(defn relay-body-clicks [e store element pages]
  (let [path (some->> (.-target e) a-element .-href get-path)]
    (when-let [location (some->> path (router/resolve-route pages))]
      (.preventDefault e)
      (if (or e.ctrlKey e.metaKey)
        (.open js/window path "_blank")
        (navigator/go-to-location store element location)))))

(defn go-to-current-location [store element pages]
  (->> (router/get-current-location (:config @store) pages)
       (navigator/go-to-location store element)))

(defn bootup [{:keys [store element event-bus pages] :as app}]
  (let [config (:config @store)]
    (logger/configure-logging)
    (log/info "Starting app with config" config)

    (swap! store assoc
           ::bootup-at (.getTime (js/Date.))
           :locale (:default-locale config :en))

    ;; handle window.onerror

    (when (:log-event-bus-messages? config)
      (bus/subscribe event-bus ::app (fn [topic & args]
                                       (if args
                                         (log/debug topic args)
                                         (log/debug topic)))))


    (set! js/window.onpopstate (fn [] (go-to-current-location store element @pages)))

    (js/document.body.addEventListener "click" #(relay-body-clicks % store element @pages))

    (win/keep-size-up-to-date store)

    (add-watch store ::render (fn [_ _ _ state]
                                (page/render-current-location app state)))

    (go-to-current-location store element @pages)
    ))
