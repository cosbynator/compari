(in-ns 'lda.core)

(require '[clojure.data.xml :as xml])
(require '[clojure.java.io :as io])
(require '[clojure.string :as string])

(defn filter-tag [tag xml] (filter #(= tag (:tag %)) xml))
(defn first-tag [tag xml] (first (filter-tag tag xml)))

(defn article-title [page] (:content (first-tag :title (:content page))))
(defn article-redirect [page] (:title (:attrs (first-tag :redirect (:content page)))))
(defn article-id [page] (->> page :content (first-tag :id) :content))
(defn article-text [page] (->> page :content (first-tag :revision) :content (first-tag :text) :content (apply str)))

(defn page-seq [rdr]
  (->>
    (:content (xml/parse rdr :coalescing false))
    (filter-tag :page)
    ))


(defn process-page [page]
  (let [title (article-title page)
        redirect (article-redirect page)
        id (article-id page)
        text (article-text page)
        ]

    (do
      (println id)
      )
    ))




(defn bzip2-reader ^java.io.Reader [file]
  (io/reader
    (.createCompressorInputStream  (org.apache.commons.compress.compressors.CompressorStreamFactory.)
      (org.apache.commons.compress.compressors.CompressorStreamFactory/BZIP2)
      (io/input-stream file)
      )
    )
  )

(defn file-reader ^java.io.Reader [^String file]
  (cond
    (.endsWith file ".bz2") (bzip2-reader file)
    :else (io/reader file)
  )
 )

(defn wiki-dump-articles [filename] (filter #(string/blank? (article-redirect %)) (page-seq (file-reader filename))))
(defn wiki-dump-pages [filename] (page-seq (file-reader filename)))


