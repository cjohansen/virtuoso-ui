(ns virtuoso.login-page.components
  (:require [dumdom.core :as d]
            [ui.components.button :refer [button]]
            [ui.components.input :refer [input]]
            [virtuoso.components.brain :as brain]))

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
                  :max-width 500
                  :margin "0 auto"}}
    [:div {:style {:padding "40px 20px 0"}}
     (brain/brain {:id "logo"})]
    [:div.vs-m {:style {:padding "30px 20px 40px"}}
     [:form.vs-s {:onSubmit (-> data :form :actions)}
      (for [input-data (-> data :form :inputs)]
        [:div.mod (input input-data)])
      [:div.mod (button (-> data :form :button))]]
     [:p.small.mod (:text data)]]
    [:div {:style {:padding 20
                   :max-width 300
                   :margin "0 auto"}}]]])
