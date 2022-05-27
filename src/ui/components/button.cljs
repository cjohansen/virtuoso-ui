(ns ui.components.button
  (:require [ui.components.spinner :refer [spinner]]))

(defn button [{:keys [id text type href disabled? actions] :as props}]
  [(if (#{"button" "submit"} type) :button :a)
   (merge
    {:className (str "button text-m"
                     (when disabled? " button-disabled"))
     :onClick actions}
    (when type {:type type})
    (when (and type disabled?) {:disabled "true"})
    (when id {:id id})
    (when href {:href href}))
   (when (:spinner? props)
     (spinner))
   text])
