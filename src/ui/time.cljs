(ns ui.time
  (:import [goog date]
           [goog.i18n DateTimeFormat DateTimeSymbols_en DateTimeSymbols_no TimeZone]))

(set! (.-STANDALONESHORTWEEKDAYS DateTimeSymbols_no) #js ["søn" "man" "tir" "ons" "tor" "fre" "lør"])

(def ^:private i18n-syms
  {:nb DateTimeSymbols_no
   :en DateTimeSymbols_en})

(defn format-ordinal
  "Format number with ordinal suffix"
  [locale num]
  (condp = locale
    :nb (str num ".")
    :en (let [under-ten-or-over-twenty (or (< num 10) (< 20 num))]
          (str num
               (cond
                 (and (= (mod num 10) 1) under-ten-or-over-twenty) "st"
                 (and (= (mod num 10) 2) under-ten-or-over-twenty) "nd"
                 (and (= (mod num 10) 3) under-ten-or-over-twenty) "rd"
                 :default "th")))
    num))

(defn format-datetime
  "Format date with the provided pattern. Pattern should be a valid
   DateTimeFormat pattern."
  [locale pattern date]
  (if-let [[_ before _ after] (re-find #"(.*)(o)(.*)" pattern)]
    (str (format-datetime locale before date)
         (format-ordinal locale (.getDate date))
         (format-datetime locale after date))
    (.format (DateTimeFormat. pattern (i18n-syms locale)) date)))

(defn now []
  (js/Date.))

(defn timestamp []
  (.getTime (now)))

(defn to-long [d]
  (.getTime d))

(defn before? [a b]
  (let [at (some-> a .getTime)
        bt (some-> b .getTime)]
    (when (and at bt)
      (< at bt))))

(def ^:private MINUTE_THRESHOLD (* 45 1000))
(def ^:private HOUR_THRESHOLD (* 50 60 1000))
(def ^:private DAY_THRESHOLD (* 22 60 60 1000))
(def ^:private MONTH_THRESHOLD (* 25.5 24 60 60 1000))
(def ^:private MINUTES (* 60 1000))
(def ^:private HOURS (* 60 MINUTES))
(def ^:private DAYS (* 24 HOURS))
(def ^:private MONTHS (* 30.5 DAYS))

(defn- rounded-diff [diff unit]
  (Math/floor (/ (+ diff (/ unit 2)) unit)))

(defn describe-duration [from to]
  (when (and from to)
    (let [diff (- (to-long to) (to-long from))]
      (cond
        (< diff MINUTE_THRESHOLD) [:i18n/k :duration/less-than-a-minute]
        (< diff HOUR_THRESHOLD) [:i18n/k :duration/minutes (rounded-diff diff MINUTES)]
        (< diff DAY_THRESHOLD) [:i18n/k :duration/hours (rounded-diff diff HOURS)]
        (< diff MONTH_THRESHOLD) [:i18n/k :duration/days (rounded-diff diff DAYS)]
        :default [:i18n/k :duration/months (rounded-diff diff MONTHS)]))))
