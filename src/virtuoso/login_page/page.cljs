(ns virtuoso.login-page.page
  (:require [virtuoso.login-page.components :refer [login-page-component]]
            [clojure.string :as str]))

(defn valid-email? [email]
  (and (string? email)
       (re-find #"^[^@]+@[^\.]+\.[^\.]+" (str/trim email))))

(defn prepare-login-page [state location]
  (let [email (get-in state [:transient location :email])
        valid? (valid-email? email)]
    {:title [:i18n/k ::title]
     :logo {:logo/kind :virtuoso/name
            :url "/images/virtuoso-logo.svg"
            :color "var(--silver-chalice)"
            :theme :theme/blue-gradient}
     :symbol {:logo/kind :virtuoso/gradient
              :url "/images/virtuoso-greyscale.svg"}
     :text [:i18n/k ::form-text]
     :input {:placeholder [:i18n/k ::input-label]
             :onInput [[:actions/assoc-in [:transient location :email] :event/target.value]]}
     :button {:text [:i18n/k ::button]
              :disabled? (not valid?)
              :actions (when valid?
                         [[:actions/command :login/request-otp email]])}}))

(defn register-actions [store event-bus]
  )

(def page
  {:location/route ["login"]
   :location/page-id :login-page
   :login-page? true
   :page-title ::page-title
   :prepare #'prepare-login-page
   :component #'login-page-component
   :register-actions #'register-actions})
