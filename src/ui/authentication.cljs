(ns ui.authentication
  (:require [clojure.string :as str]
            [ui.time :as time]))

(defn decode-base64-utf8
  "atob does not safely decode UTF-8 strings. This little incantation, however,
  does. `encode` is of course deprecated since JS 1.5, but is still around in
  browsers, and likely will for a long time. Should it go missing, it is also
  possible to do `(.decode (js/TextDecoder.) (js/Uint8Array.from (js/atob s)))`,
  although that only works in newer browsers."
  [s]
  (-> s
      js/atob
      js/escape
      js/decodeURIComponent))

(defn decode-safely [base64json]
  (try
    (-> base64json
        decode-base64-utf8
        js/JSON.parse
        (js->clj :keywordize-keys true))
    (catch :default e
      nil)))

(defn decode-jwt [token]
  (when (string? token)
    ;; JWT uses base64Url encoding, which uses - for + and _ for /
    ;; JavaScript does not understand base64Url, but we can replace those
    ;; characters before decoding.
    (let [[header claims sig] (-> token
                                  (str/replace #"\-" "+")
                                  (str/replace #"_" "/")
                                  (str/split #"\."))]
      {:header (decode-safely header)
       :claims (decode-safely claims)
       :sig sig})))

(defn token-expired? [{:keys [exp]} & [now]]
  (let [now (or now (time/timestamp))]
    (<= (* exp 1000) now)))

(defn authenticated? [state]
  (not (nil? (:token state))))

(defprotocol Authenticator
  (<ensure-authenticated! [_ app]))
