(ns lda.core)
(set! *warn-on-reflection* true)

(use 'flatland.protobuf.core)
(use 'clj-progress.core)
(require '[clojure.string :as string])
(require '[clojure.java.io :as io])

(import 'com.thomasdimson.wikipedia.lda.java.MarkupCleaner)
(import 'com.thomasdimson.wikipedia.lda.java.WikipediaHandler)
(import 'com.thomasdimson.wikipedia.lda.java.IntermediateTSPRNode)
(import 'com.thomasdimson.wikipedia.lda.java.TopicSensitivePageRank)
(import 'com.thomasdimson.wikipedia.Data$DumpPage)
(import 'com.thomasdimson.wikipedia.Data$WikiGraphNode)

(def num-docs 7508922)

; Macro zone
(defmacro dbg-b [str & body] `(do (println ~str) ~@body))
(defmacro dbg-a [str & body] `(let [x# ~@body] (do (println ~str) x#)))

; Make graph out of wikipedia
(def WikiGraphNode (protodef Data$WikiGraphNode))

(defn valid-article? [^Data$DumpPage page] (not (.hasRedirect page)))
(defn textual-links [^String wiki-text] (map #(% 1) (re-seq #"\[\[(?:[^|\]]*\|)?([^\]]+)\]\]" wiki-text)))

(defn extract-redirects [pages]
  (into {} (for [^Data$DumpPage page pages :when (.hasRedirect page)]
             [(.getTitle page) (.getRedirect page)]))
)
(defn extract-title-id-mapping [pages]
    (into {} (for [^Data$DumpPage page pages :when (valid-article? page)]
               [(.getTitle page) (.getId page)]))
    )

(defn link-text-to-id [^String link-text redirect-map title-map]
  (title-map (if-let [redirected (redirect-map link-text)] redirected link-text))
)

(defn dump-file-iterator [^String input-file] (iterator-seq (WikipediaHandler/newStructuredDumpIterator input-file)))

(defn wiki-graph-nodes [^String input-file]
  (let [redirects (extract-redirects (dump-file-iterator input-file))
        title-map (extract-title-id-mapping (dump-file-iterator input-file))]
    (for [^Data$DumpPage page (dump-file-iterator input-file)
             :when (valid-article? page)
             :let [link-ids (into [] (filter identity (map #(link-text-to-id % redirects title-map) (textual-links (.getText page)))))]
           ]
      (protobuf WikiGraphNode :id (.getId page) :title (.getTitle page) :edges link-ids)
    )
  )
)

(defn write-wiki-graph-nodes [^String input-file ^String output-file]
  (with-open [w (io/output-stream output-file)]
    (apply (partial protobuf-write w) (wiki-graph-nodes input-file))
  )
)

(defn read-wiki-graph-nodes [^String input-file] (protobuf-seq WikiGraphNode input-file))

(defn make-intermediate-tspr-nodes [^String input-file lda-map]
  (do
    (init "Creating intermediate TSPR nodes" num-docs)
    (done
      (map-indexed
        (fn [idx graph-node]
          (tick (IntermediateTSPRNode. idx
            (int (:id graph-node)) (:title graph-node) (into-array Long/TYPE (map long (:edges graph-node))) (get lda-map (:title graph-node))
          ))
        )
        (read-wiki-graph-nodes input-file)
    ))
  )
)

(defn read-lda-map [^String lda-file]
  (let [line2assoc
          (fn [^String line]
            (let  [split (string/split line #"\t")
                   sequential-id (get split 0)
                   title (get split 1)
                   topics (map #(Integer/parseInt %) (take-nth 2 (drop 2 split)))
                   probs (map #(Double/parseDouble %) (take-nth 2 (drop 3 split)))
                   ]
            [title (into-array Double/TYPE (map #(get % 1) (sort (cons [100000 1.0] (map vector topics probs)))))]
              )
            )]
      (with-open [r (io/reader lda-file)]
        (do
          (init "Reading LDA Map" num-docs)
          (done (into {} (map (comp tick line2assoc) (rest (line-seq r)))))
        )
       )
  )
)

(defn tspr [^String input-file ^String lda-file ^String output-file]
  (let [
         lda-map (dbg-b "Reading lda map" (read-lda-map lda-file))
         intermediate-vector (dbg-b "Reading intermediate nodes" (java.util.ArrayList. (make-intermediate-tspr-nodes input-file lda-map)))
       ]
    (do
      (TopicSensitivePageRank/rankInPlace intermediate-vector)
      (with-open [w (io/writer output-file)]
        (doseq [^IntermediateTSPRNode node intermediate-vector]
          (.writeDelimitedTo (.toProto node) w)
      ))
    )
  )
)

; Prepare for LDA
(defn clean-wiki-stream [^String input-file ^String whitelist-file ^String output-file]
  (let [^MarkupCleaner markup-cleaner (MarkupCleaner. MarkupCleaner/STOP_WORDS (MarkupCleaner/readWhitelist whitelist-file 1))
        dump-page-clean (fn [^Data$DumpPage page] [page (.cleanMarkup markup-cleaner (.getText page))])
        ]
    (with-open [^java.io.BufferedOutputStream os (java.io.BufferedOutputStream. (java.io.FileOutputStream. output-file))]
      (doseq [clean_page (pmap dump-page-clean (filter valid-article? (iterator-seq (WikipediaHandler/newStructuredDumpIterator input-file))))
              :let [^Data$DumpPage page (get clean_page 0)
                    ^String clean (get clean_page 1)]
              ]
        (.writeDelimitedTo (.build (.setText (Data$DumpPage/newBuilder page) clean)) os)
        )
      )
  ))
