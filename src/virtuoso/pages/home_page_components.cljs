(ns virtuoso.pages.home-page-components
  (:require [dumdom.core :as d]))

(d/defcomponent home-page-component [data]
  [:div
   [:h1.h1 (:title data)]
   [:p (:text data)]])
