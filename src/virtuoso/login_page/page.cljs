(ns virtuoso.login-page.page
  (:require [clojure.string :as str]
            [ui.authentication :as auth]
            [ui.event-bus :as bus]
            [ui.misc :as misc]
            [ui.picard :as picard]
            [virtuoso.login-page.components :refer [login-page-component]]))

(defn valid-email? [email]
  (and (string? email)
       (re-find #"^[^@]+@[^\.]+\.[^\.]+" (str/trim email))))

(defn valid-otp? [otp]
  (and (string? otp)
       (re-find #"^\d{6}$" otp)))

(defn requested-otp? [state email]
  (let [{:keys [status command]}
        (picard/get-execution state :login/request-otp)]
    (and (= :success status)
         (= email (second command)))))

(defn with-form-action [form]
  (assoc form :actions (:actions (:button form))))

(defn prepare-login-form [state location {:keys [email otp]}]
  (let [requested-otp? (requested-otp? state email)
        in-progress? (or (picard/in-progress? state :login/request-otp)
                         (picard/in-progress? state :login/authenticate))]
    (with-form-action
      {:inputs (->> [{:placeholder [:i18n/k ::input-label]
                      :disabled in-progress?
                      :value email
                      :onInput [[:actions/assoc-in [:transient location :email] :event/target.value]]}
                     (when (and email requested-otp?)
                       {:placeholder [:i18n/k ::otp-label]
                        :disabled in-progress?
                        :onInput [[:actions/assoc-in [:transient location :otp] :event/target.value]]})]
                    (remove nil?))
       :button (if requested-otp?
                 (let [valid? (valid-otp? otp)]
                   {:text [:i18n/k ::button]
                    :disabled? (or (not valid?) in-progress?)
                    :spinner? in-progress?
                    :actions (when valid?
                               [[:actions/public-command :login/authenticate email otp]])})
                 (let [valid? (valid-email? email)]
                   {:text [:i18n/k ::button]
                    :disabled? (or (not valid?) in-progress?)
                    :spinner? in-progress?
                    :actions (when valid?
                               [[:actions/public-command :login/request-otp email (:locale state)]])}))})))

(defn prepare-login-page [state location]
  {:title [:i18n/k ::title]
   :logo {:logo/kind :virtuoso/name
          :url "/images/virtuoso-logo.svg"
          :color "var(--silver-chalice)"
          :theme :theme/blue-gradient}
   :symbol {:logo/kind :virtuoso/gradient
            :url "/images/virtuoso-greyscale.svg"}
   :text [:i18n/k ::form-text]
   :form (prepare-login-form state location (get-in state [:transient location]))})

(defn complete-authentication [state {:keys [data]}]
  (let [{:keys [claims]} (auth/decode-jwt (:token data))]
    [[:actions/assoc-in
      [:token] (:token data)
      [:token-info] claims]]))

(defn complete-login [state res]
  (concat (complete-authentication state res)
          [[:actions/go-to-location {:location/page-id :virtuoso.pages/home-page}]]))

(defn register-actions [{:keys [store event-bus]}]
  (bus/subscribe
   event-bus ::actions
   [::picard/complete-execution :login/authenticate]
   (misc/event-emitter event-bus #(apply complete-login @store %&)))

  (bus/subscribe
   event-bus ::actions
   [::picard/complete-execution :login/refresh-token]
   (misc/event-emitter event-bus #(apply complete-authentication @store %&))))

(def page
  {:location/route ["login"]
   :location/page-id :virtuoso.pages/login-page
   :login-page? true
   :page-title ::page-title
   :prepare #'prepare-login-page
   :component #'login-page-component
   :register-actions #'register-actions})
