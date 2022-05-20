(ns ui.i18n
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [ui.time :as time]))

(defn flatten-dictionary [m]
  (->> m
       (mapcat (fn [[k v]]
                 (if (map? v)
                   (map (fn [[nested-k nested-v]]
                          [(keyword (name k) (name nested-k)) nested-v]) v)
                   [[k v]])))
       (into {})))

(defn validate-dictionary [dictionary]
  (doseq [[locale dict] dictionary]
    (doseq [[k v] dict]
      (when (->> (tree-seq coll? identity v)
                 (some #{:i18n/k}))
        (throw (ex-info "Dictionary key uses recursive :i18n/k, will not work as expected."
                        {:locale locale :k k :v v})))))
  dictionary)

(def str-int-re #"\{\{([^\}]+)\}\}")

(defn get-string-placeholders [s]
  (->> (re-seq #"\{\{([^\}]+)\}\}" s)
       (map (fn [[_ k]] [(str "{{" k "}}") (keyword k)]))))

(defn interpolate-string [s placeholders interpolations]
  (->> placeholders
       (reduce (fn [s [ph k]]
                 (str/replace s ph (get interpolations k)))
               s)))

(defn interpolatable-string? [s]
  (and (string? s)
       (not (empty? (get-string-placeholders s)))))

(defn process-val [locale dictionary k-fns v data]
  (->> v
       (walk/postwalk
        (fn [x]
          (if (and (vector? x)
                   (contains? k-fns (first x)))
            (apply (get k-fns (first x)) locale data (rest x))
            x)))
       (walk/postwalk
        (fn [x]
          (if (string? x)
            (interpolate-string x (get-string-placeholders x) data)
            x)))))

(defn prepare-dict-val [locale dictionary k-fns v]
  (let [syms (tree-seq coll? identity v)]
    (if (or (some interpolatable-string? syms)
            (some (set (keys k-fns)) syms))
      (partial process-val locale dictionary k-fns v)
      v)))

(defn inline-aliases [dictionary]
  (->> dictionary
       (map (fn [[k v]]
              [k (walk/postwalk (fn [x]
                                  (get dictionary x x)) v)]))))

(defn compile-dictionary [locale k-fns dictionary]
  (->> dictionary
       inline-aliases
       (map (fn [[k v]]
              [k (prepare-dict-val locale dictionary k-fns v)]))
       (into {})))

(defn prep-dictionaries [dictionaries & [{:keys [k-fns]}]]
  (->> dictionaries
       (map (fn [[locale dictionary]]
              [locale
               (if (map? dictionary)
                 (->> dictionary
                      flatten-dictionary)
                 dictionary)]))
       validate-dictionary
       (map (fn [[locale v]] [locale (compile-dictionary locale k-fns v)]))
       (into {})))

(defn lookup [dictionaries locale k & [data]]
  (let [v (get-in dictionaries [locale k])]
    (if (fn? v)
      (v data)
      v)))

(defn tr [dictionaries locale data]
  (walk/prewalk
   (fn [x]
     (if (and (vector? x) (= :i18n/k (first x)))
       (apply lookup dictionaries locale (drop 1 x))
       x))
   data))

(defn pluralize [locale n & plurals]
  (-> (nth plurals (min (if (number? n) n 0) (dec (count plurals))))
      (interpolate-string [["{{n}}" :n]] {:n n})))

(defn interpolate [locale data v]
  (get data v))

(defn format-date [locale data format date]
  (when date (time/format-datetime locale format date)))
