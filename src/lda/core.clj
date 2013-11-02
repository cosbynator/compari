(ns lda.core)
(set! *warn-on-reflection* true)

(use 'flatland.protobuf.core)
(require '[clojure.string :as string])
(require '[clojure.java.io :as io])

(import 'com.thomasdimson.wikipedia.lda.java.MarkupCleaner)
(import 'com.thomasdimson.wikipedia.lda.java.WikipediaHandler)
(import 'com.thomasdimson.wikipedia.lda.java.IntermediateTSPRNode)
(import 'com.thomasdimson.wikipedia.lda.java.TopicSensitivePageRank)
(import 'com.thomasdimson.wikipedia.lda.java.TopicSensitivePageRank$TSPRType)
(import 'com.thomasdimson.wikipedia.Data$DumpPage)
(import 'com.thomasdimson.wikipedia.Data$WikiGraphNode)

; Macro zone
(defmacro dbg-b [str & body] `(do (println ~str) ~@body))
(defmacro dbg-a [str & body] `(let [x# ~@body] (do (println ~str) x#)))

; Make graph out of wikipedia
(def WikiGraphNode (protodef Data$WikiGraphNode))

(defn valid-article? [^Data$DumpPage page] (not (.hasRedirect page)))
(defn textual-links [^String wiki-text] (map #(% 1) (re-seq #"\[\[(?:[^|\]]*\|)?([^\]]+)\]\]" wiki-text)))
(defn infobox-type [^String wiki-text] (first (map #(string/trim (string/replace (% 1) #"\s+" " "))
                                                      (re-seq #"\{\{[Ii]nfobox\s+([a-zA-Z0-9_\s]+)" wiki-text))))

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
  (let [redirects (dbg-b "Extracting redirects" (extract-redirects (dump-file-iterator input-file)))
        title-map (dbg-b "Extracting id mapping" (extract-title-id-mapping (dump-file-iterator input-file)))]
    (dbg-b "Creating graph nodes"
      (for [^Data$DumpPage page (dump-file-iterator input-file)
             :when (valid-article? page)
             :let [link-ids (into [] (distinct (filter identity (map #(link-text-to-id % redirects title-map) (textual-links (.getText page))))))]
           ]
      (protobuf WikiGraphNode :id (.getId page) :title (.getTitle page) :edges link-ids :infobox_type (infobox-type (.getText page)))
    ))
  )
)

(defn write-wiki-graph-nodes [^String input-file ^String output-file]
  (with-open [w (io/output-stream output-file)]
    (apply (partial protobuf-write w) (wiki-graph-nodes input-file))
  )
)

(defn read-wiki-graph-nodes [^String input-file] (protobuf-seq WikiGraphNode input-file))

(defn make-intermediate-tspr-nodes [^String input-file lda-map]
    (map-indexed
      (fn [idx graph-node]
        (IntermediateTSPRNode. idx
          (int (:id graph-node)) (:title graph-node) (into-array Long/TYPE (map long (:edges graph-node)))
          (get lda-map (:title graph-node)) (:infobox_type graph-node)
        )
      )
      (read-wiki-graph-nodes input-file)
  )
)

(defn tspr [^String input-file ^String lda-file ^String output-file ^Double convergence algorithm-type]
  (let [
         lda-map (dbg-b "Reading lda map" (TopicSensitivePageRank/readLDAMap lda-file))
         intermediate-vector (dbg-b "Reading intermediate nodes" (java.util.ArrayList.
                                                                   (make-intermediate-tspr-nodes input-file lda-map)))
         algo (if (= (string/lower-case algorithm-type) "lspr") TopicSensitivePageRank$TSPRType/LSPR TopicSensitivePageRank$TSPRType/TSPR)
       ]
    (do
      (println "Using algorithm" algo)
      (TopicSensitivePageRank/rankInPlace intermediate-vector convergence algo)
      (with-open [w (io/output-stream output-file)]
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
