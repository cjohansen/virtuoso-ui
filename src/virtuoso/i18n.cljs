(ns ^:figwheel-hooks virtuoso.i18n
  (:require [ui.i18n :as i18n]
            [virtuoso.i18n.en :as en]
            [virtuoso.i18n.nb :as nb]))

(defn init-dictionaries []
  (i18n/prep-dictionaries
   {:nb nb/dictionary
    :en en/dictionary}
   {:k-fns {:i18n/i i18n/interpolate
            :i18n/date i18n/format-date
            :i18n/plural i18n/pluralize}}))
