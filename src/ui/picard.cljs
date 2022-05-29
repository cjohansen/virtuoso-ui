(ns ui.picard
  "Captain of the USS Enterprise and Lord of the Facepalm, Jean-Luc Picard is the
  UI commander. All commands sent to backends should be sent via
  picard/<command! or indirectly via the :commmand bus event.

  A command is a vector where the first element is considered the command
  identifier:

  ```clj
  [[:update-item id] item]
  ```

  The identifier here is: `[:update-item id]`

  An execution is one instance of executing a command.

  Only one execution with the same identifier can be in progress at the same
  time. Only one complete execution will be kept for the same identifier.

  Picard keeps its state in store like this:

  ```clj
  {:picard/executions
   {identifier {:started-at #inst \"2022\"
                :completed-at #inst \"2022\"
                :command command
                :status :success
                :result :yay}}}
  ```

  Use the convenience functions `in-progress?`, `successful?`, and
  `unsuccessful?` instead of digging directly into this data structure.

  Picard also keeps some information about the last 5 executions in its log,
  which is mainly kept as useful data while investigating frontend errors in
  production.

  ```clj
  {:picard/log
   {identifier '([:success #inst \"2022\"]
                 [:failure #inst \"2022\"]
                 [:failure #inst \"2022\"]
                 [:failure #inst \"2022\"])}}
  ```"
  (:require [cljs.core.async :refer [<! put!]]
            [cljs.tools.reader.edn :as edn]
            [taoensso.timbre :as log]
            [ui.chanel :as chanel]
            [ui.event-bus :as bus])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn get-execution [state identifier]
  (get-in state [:picard/executions identifier]))

(defn get-log [state identifier]
  (get-in state [:picard/log identifier]))

(defn unsuccessful? [state identifier]
  (#{:failure :error :bad-request :unauthorized}
   (:status (get-execution state identifier))))

(defn successful? [state identifier]
  (#{:success}
   (:status (get-execution state identifier))))

(defn in-progress? [state identifier]
  (#{:in-progress}
   (:status (get-execution state identifier))))

(defn ms-since-start [state identifier]
  (when-let [execution (get-execution state identifier)]
    (- (:now state) (:started-at execution))))

(defn get-fetch-params [req]
  (cond-> (dissoc req :token :url :edn-params)
    (:token req) (assoc-in [:headers "Authorization"] (str "Bearer " (:token req)))
    (:token req) (assoc :credentials "same-origin")
    (:edn-params req) (assoc-in [:headers "Content-Type"] "application/edn")
    (:edn-params req) (assoc :body (pr-str (:edn-params req)))))

(defn parse-response-body [res body]
  (or
   (when (re-find #"\bedn\b" (.get (.-headers res) "content-type"))
     (try
       (edn/read-string body)
       (catch :default e
         nil)))
   body))

(defn request [req]
  (let [ch (chanel/chan ::fetch)]
    (-> (js/fetch (:url req) (clj->js (get-fetch-params req)))
        (.then
         (fn [res]
           (-> (.text res)
               (.then
                (fn [body]
                  (put! ch {:status (.-status res)
                            :headers (->> (.-headers res)
                                          .keys
                                          (map (fn [k]
                                                 [k (.get (.-headers res) k)]))
                                          (into {}))
                            :body (parse-response-body res body)})
                  (chanel/close! ch)))))))
    ch))

(defn execute-command! [store event-bus opt [identifier :as command]]
  (let [start-time (js/Date.)]
    (swap! store assoc-in [:picard/executions identifier] {:status :in-progress
                                                           :started-at start-time
                                                           :command command})
    (go
      (let [response (<! (let [state (cond-> @store
                                       (not (:include-token? opt)) (dissoc :token))]
                           (request
                            {:method :post
                             :url (str (-> state :config :command-host) "/api/command")
                             :headers {"X-Session-Id" (-> state :session :id)
                                       "X-Correlation-Id" (str (random-uuid))
                                       "Accept" "application/edn"}
                             :token (:token state)
                             :edn-params {:command command}})))
            result (if (:status (:body response))
                     (:body response)
                     {:status :error :result :invalid-http-response})]
        (swap! store (fn [state]
                       (let [completed-at (js/Date.)]
                         (-> state
                             (assoc-in [:picard/executions identifier]
                                       (assoc result
                                              :command command
                                              :started-at start-time
                                              :completed-at completed-at))
                             (update-in [:picard/log identifier]
                                        #(take 5 (cons [(:status result) completed-at] %)))))))
        (bus/publish event-bus [::complete-execution identifier] result)))))

(defn <command! [store event-bus opt [identifier :as command]]
  (let [return-ch (chanel/chan [::command identifier])]
    (if (in-progress? @store identifier)
      (log/warn "Duplicate command, already in progress, waiting for original" command)
      (execute-command! store event-bus opt command))
    (bus/subscribe-until
     event-bus
     [::complete-execution identifier]
     (fn [result]
       (put! return-ch result)
       (chanel/close! return-ch)
       :done))
    return-ch))
