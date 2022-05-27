(ns virtuoso.login-page.components
  (:require [dumdom.core :as d]
            [ui.components.button :refer [button]]
            [virtuoso.components.logos :as logos]))

(d/defcomponent login-page-component [data]
  [:div {:style {:position "absolute"
                 :top 0
                 :bottom 0
                 :left 0
                 :right 0
                 :flex-direction "column"
                 :display "flex"}}
   [:div {:style {:display "flex"
                  :flex-direction "column"
                  :flex-grow 1
                  :justify-content "space-between"
                  :max-width 600
                  :margin "0 auto"}}
    [:div {:style {:padding "40px 30px 0"}}
     (logos/render (:logo data))]
    [:div.vs-m {:style {:padding "30px 20px 40px"}}
     [:form.vs-s
      [:div.mod [:input.input (:input data)]]
      [:div.mod (button (:button data))]]
     [:p.small.mod (:text data)]]
    [:div {:style {:padding 20
                   :max-width 300
                   :margin "0 auto"}}
     (logos/render (:symbol data))]]])
