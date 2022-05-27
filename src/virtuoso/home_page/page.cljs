(ns virtuoso.home-page.page
  (:require [virtuoso.home-page.components :refer [home-page-component]]))

(defn prepare-home-page [state location]
  {:title [:i18n/k ::title]
   :text [:i18n/k ::text]})

(def page
  {:location/route []
   :location/page-id :virtuoso.pages/home-page
   :default-page? true
   :page-title ::page-title
   :prepare #'prepare-home-page
   :component #'home-page-component})
