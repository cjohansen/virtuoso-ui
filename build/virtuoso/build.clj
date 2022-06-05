(ns virtuoso.build
  (:require [cljs.build.api :as compiler]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [dumdom.core :as dumdom]
            [optimus.assets :as assets]
            [optimus.assets.creation]
            [optimus.export]
            [optimus.html]
            [optimus.optimizations :as optimizations]
            [ui.svg :as svg]))

(defmacro with-time [name & body]
  `(let [before# (System/currentTimeMillis)]
     (print ~name "... ") (flush)
     (let [res# (do ~@body)]
       (println (- (System/currentTimeMillis) before#) "ms")
       res#)))

(defn find-included-icons [path]
  (->> (file-seq (io/file (str path "/out")))
       (filter #(re-find #"target/out/(ui|virtuoso).*\.cljs$" (.getPath %)))
       (mapcat (fn [file]
                 (map second (re-seq #":(streamline[^\s}]+)" (slurp file)))))
       (map keyword)
       set))

(comment
  (find-included-icons "target")
  )

(defn lookup-css-var [css var]
  (let [[_ v] (re-find (re-pattern (str var ": ([^;]*)")) css)
        [_ var] (re-find #"var\((.*)\)" v)]
    (if var
      (lookup-css-var css var)
      v)))

(defn get-theme-color []
  (lookup-css-var (slurp (io/resource "public/css/virtuoso.css")) '--theme-color))

(comment
  (get-theme-color)
  )

(defn render-embeddable-icon [id]
  (let [[_ g] (->> (str "public" (svg/get-url id))
                   (io/resource )
                   slurp
                   (re-find #"(?is)<svg[^>]*>(.*)</svg>"))]
    (str/replace (str/trim g) #"id=\"root\"" (str "id=\"" (svg/id-str id) "\""))))

(comment
  (render-embeddable-icon :streamline-bold/music-audio.playlist-edit)
  )

(defn load-file-asset [bundle file]
  [{:path (str "/" bundle)
    :last-modified (optimus.assets.creation/last-modified (io/as-url file))
    :bundle bundle
    :contents (slurp file)}])

(defn load-bundles [path]
  (concat
   (assets/load-bundles
    "public"
    {"styles.css" ["/css/virtuoso.css"]})
   (load-file-asset "app.js" (io/file (str path "/app.js")))
   (load-file-asset "app.js.map" (io/file (str path "/app.js.map")))))

(defn fix-source-map-reference [assets]
  (let [source-map-path (->> assets
                             (filter (comp #{"app.js.map"} :bundle))
                             first
                             :path)]
    (->> assets
         (map (fn [asset]
                (cond-> asset
                  (= "app.js" (:bundle asset))
                  (update :contents str/replace
                          #"//# sourceMappingURL=app.js.map"
                          (str "//# sourceMappingURL=" source-map-path))))))))

(defn load-optimized-assets [path]
  (-> (load-bundles path)
      (optimizations/minify-css-assets {})
      (optimizations/inline-css-imports)
      (optimizations/concatenate-bundles {})
      (optimizations/add-cache-busted-expires-headers)
      (->> (remove :bundled)
           (remove :outdated)
           (remove #(and (re-find #"\.css$" (:path %))
                         (not (re-find #"styles.css" (:path %))))))
      fix-source-map-reference))

(defn export-assets [path assets]
  (optimus.export/save-assets assets (str path "/public")))

(defn build-index [assets icons]
  [:html
   [:head
    [:title "Virtuoso"]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"}]
    [:meta {:charset "UTF-8"}]
    [:meta {:name "theme-color" :content (get-theme-color)}]
    (optimus.html/link-to-css-bundles {:optimus-assets assets} ["styles.css"])]
   [:body
    [:div {:id "app"}]
    (when (seq icons)
      [:svg {:xmlns "http://www.w3.org/2000/svg" :style {:display "none"}}
       (map render-embeddable-icon icons)])
    (optimus.html/link-to-js-bundles {:optimus-assets assets} ["app.js"])]])

(defn generate-index-html [path assets icons]
  (let [dir (str path "/public")]
    (.mkdirs (java.io.File. dir))
    (spit (str dir "/index.html")
          (str "<!DOCTYPE html>\n"
               (dumdom/render-string (build-index assets icons))))))

(defn compile-cljs [path]
  (compiler/build
   (compiler/inputs "src" "resources" "prod")
   {:main "virtuoso.prod"
    :optimizations :advanced
    :parallel-build true
    :source-map (str path "/app.js.map")
    :output-to (str path "/app.js")
    :output-dir (str path "/out")
    :externs ["resources/externs.js"]
    :foreign-libs [{:file "lib/sse.js"
                    :file-min "lib/sse.min.js"
                    :provides ["sse"]}]}))

(defn build [& [args]]
  (let [path (str/replace (or (get args '--path) "target") #"/$" "")]
    (with-time "Compile CLJS sources"
      (compile-cljs path))
    (let [assets (with-time "Load and optimize assets"
                   (load-optimized-assets path))]
      (with-time "Build index.html"
        (generate-index-html path assets (find-included-icons path)))
      (with-time "Export assets"
        (export-assets path assets)))))

(comment

  (build)

)
