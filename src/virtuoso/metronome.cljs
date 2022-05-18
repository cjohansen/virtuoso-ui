(ns ui.metronome)

(defn create-sine-wave [audio-ctx frequency]
  (let [oscillator (.createOscillator audio-ctx)
        gain (.createGain audio-ctx)]
    (set! (.-type oscillator) "sine")
    (set! (.. oscillator -frequency -value) frequency)
    (set! (.. gain -gain -value) 0)
    (.connect oscillator gain)
    (.connect gain (.-destination audio-ctx))
    (.start oscillator 0)
    {:audio-ctx audio-ctx
     :oscillator oscillator
     :gain gain}))

(defn unmount [{:keys [oscillator gain audio-ctx]}]
  (some-> gain (.disconnect (.-destination audio-ctx)))
  (when oscillator
    (.stop oscillator)
    (.disconnect oscillator gain)))

(defn generate-bar-clicks
  ([bar]
   (generate-bar-clicks bar {:first-bar 0
                             :first-beat 0
                             :start-time 0}))
  ([bar {:keys [first-beat first-bar start-time]}]
   (let [[beats subdivision] (:time-signature bar)
         accentuate (set (:accentuate bar))
         click? (or (:click? bar) (constantly true))
         ms (/ (* 60 1000 4) (:bpm bar) subdivision)]
     (apply concat
            (for [rep (range (:repeat bar 1))]
              (let [rep-offset (* beats rep)
                    offset (+ first-beat rep-offset)]
                (->> (range beats)
                     (map (fn [beat]
                            {:bar-n (+ first-bar rep)
                             :bar-beat beat
                             :beat (+ first-beat (* rep beats) beat)}))
                     (filter #(click? % (+ offset %)))
                     (map (fn [click]
                            (cond-> (assoc click :click-at (+ start-time (* ms (+ (:bar-beat click) rep-offset))))
                              (accentuate (:bar-beat click)) (assoc :accentuate? true)))))))))))

(defn get-bar-duration [{:keys [time-signature repeat bpm]}]
  (let [beats (* (first time-signature) (or repeat 1))]
    {:beats beats
     :ms (* beats (/ (* 60 1000 4) bpm (second time-signature)))}))

(defn generate-clicks [bars & [{:keys [now first-bar first-beat]}]]
  (loop [bars (seq bars)
         res nil
         bar-n (or first-bar 0)
         beat-offset (or first-beat 0)
         start-time (or now 0)]
    (if (nil? bars)
      {:clicks res
       :bars bar-n
       :beats beat-offset
       :time start-time}
      (let [bar (first bars)
            {:keys [beats ms]} (get-bar-duration bar)]
        (recur (next bars)
               (concat res (generate-bar-clicks bar {:first-beat beat-offset
                                                     :start-time start-time
                                                     :first-bar bar-n}))
               (+ bar-n (:repeat bar 1))
               (+ beat-offset beats)
               (+ start-time ms))))))

(defn schedule-ticks [metronome opt]
  (let [{:keys [accent tick bars]} @metronome
        {:keys [clicks time bars beats]} (generate-clicks bars opt)]
    (doseq [{:keys [click-at accentuate?] :as c} clicks]
      (prn c)
      (let [{:keys [gain]} (if accentuate? accent tick)
            click-at (/ click-at 1000)]
        (.cancelScheduledValues (.-gain gain) click-at)
        (.setValueAtTime (.-gain gain) 0 click-at)
        (.linearRampToValueAtTime (.-gain gain) 1 (+ click-at 0.001))
        (.linearRampToValueAtTime (.-gain gain) 0 (+ click-at 0.001 0.01))))
    (swap! metronome assoc :tick-schedule
           (js/setTimeout
            (fn []
              (when (:running? @metronome)
                (schedule-ticks metronome {:now time
                                           :first-bar bars
                                           :first-beat beats})))
            (* 0.9 (- time (:now opt)))))))

(defn set-bpm [bpm bar]
  (assoc bar :bpm (int (* bpm (or (:tempo bar) 1)))))

(defn start-metronome [audio-ctx opts]
  (let [bpm (or (:bpm opts) 120)
        opts (-> opts
                 (update :tick-frequency #(or % 1000))
                 (update :bars (fn [bars] (map #(set-bpm bpm %) bars))))
        {:keys [tick-frequency accentuate-frequency count-in-frequency]} opts
        tick (create-sine-wave audio-ctx tick-frequency)
        accentuate (when accentuate-frequency
                     (create-sine-wave audio-ctx accentuate-frequency))
        metronome (atom (assoc opts
                               :running? true
                               :count-in (when count-in-frequency
                                           (create-sine-wave audio-ctx count-in-frequency))
                               :tick tick
                               :accent (or accentuate tick)))]
    ;; Offset slightly to avoid the very fist click occasionally being cut short
    (schedule-ticks metronome {:now (+ 5 (* 1000 (.-currentTime audio-ctx)))
                               :first-bar 0
                               :first-beat 0})
    metronome))

(defn stop-metronome [metronome]
  (let [{:keys [tick accent count-in]} @metronome]
    (unmount tick)
    (unmount accent)
    (unmount count-in))
  (when-let [t (:tick-schedule @metronome)]
    (js/clearTimeout t))
  (swap! metronome dissoc :running? :tick :accent :count-in :tick-schedule)
  nil)

(defonce audio-ctx (atom nil))
(defonce metronome (atom nil))

(defn ensure-audio-ctx []
  (when (nil? @audio-ctx)
    (reset! audio-ctx (js/AudioContext.))))

(defn start-example [bpm]
  (start-metronome @audio-ctx
                   {:bars [{:time-signature [4 4]
                            :accentuate #{0}
                            ;;:click? (constantly true)
                            ;;:repeat 2
                            }
                           {:time-signature [6 4]
                            :accentuate #{0}
                            :click? (constantly true)
                            ;;:repeat 2
                            :tempo (/ 6 4)}]
                    :bpm bpm
                    :count-in-frequency 1500
                    :tick-frequency 1000
                    :accentuate-frequency 1250}))

(defn play-example []
  (ensure-audio-ctx)
  (if-let [m @metronome]
    (do
      (stop-metronome m)
      (prn (:bpm @m))
      (reset! metronome (start-example (+ (:bpm @m) 5))))
    (reset! metronome (start-example 120))))

(defn stop-example []
  (when-let [m @metronome]
    (reset! metronome (stop-metronome m))))

(defn mount-example []
  (let [el (js/document.createElement "button")]
    (set! (.-innerHTML el) "Start")
    (.addEventListener el "click" (fn [e]
                                    (.preventDefault e)
                                    (play-example)))
    (js/document.body.appendChild el))
  (let [el (js/document.createElement "button")]
    (set! (.-innerHTML el) "Stopp")
    (.addEventListener el "click" (fn [e]
                                    (.preventDefault e)
                                    (stop-example)))
    (js/document.body.appendChild el)))

(comment

  (mount-example)

  (range 1)

  (/ (* 60 1000 4) 120 4)

  (generate-bar-clicks
   {:time-signature [4 4]
    :accentuate #{0}
    ;;:click? #(< (rand-int 10) 5)
    :repeat 4
    :bpm 120})

  (generate-bar-clicks
   {:time-signature [4 4]
    :bpm 120})

  (generate-clicks
   [{:time-signature [4 4]
     :accentuate #{0}
     :click? (constantly true)
     :repeat 2
     :bpm 120}
    {:time-signature [6 8]
     :accentuate #{0}
     :click? (constantly true)
     :repeat 2
     :bpm 90}])

  (start-metronome
   (js/AudioContext.)
   {:bars [{:time-signature [4 4]
            :accentuate #{1}
            :click? (constantly true)
            :repeat 2
            :bpm 120}
           {:time-signature [4 4]
            :accentuate #{1}
            :click? (constantly true)
            :repeat 2
            :bpm 120}]
    :count-in-frequency 1500
    :tick-frequency 1000
    :accentuate-frequency 1250})

)
