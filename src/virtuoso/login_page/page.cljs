(ns virtuoso.login-page.page
  (:require [virtuoso.login-page.components :refer [login-page-component]]))

(defn prepare-login-page [state location]
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
            :actions []}})

(def page
  {:location/route ["login"]
   :location/page-id :login-page
   :login-page? true
   :page-title ::page-title
   :prepare #'prepare-login-page
   :component #'login-page-component})
