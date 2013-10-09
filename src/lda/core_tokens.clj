(in-ns 'lda.core)

(require '[clojure.data.xml :as xml])
(require '[clojure.java.io :as io])
(require '[clojure.string :as string])

(def stop-words #{
                  "!" "," "the" "of" "and" "in" "a" "to" "-RRB-" "-LRB-" ":" "\\*" "is" "''"
                  "as" "s" "for" "by" "was" "on" "that" "with" "title" "cite" "from" "are"
                  "?" "." ";" "-" "it" "an" "or" "url" "at" "&" "his" "her" "be" "year"
                  "this" "date" "accessdate" "he" "she" "they" "were" "not" "also" "web" "%" "\\" "have"
                  "has" "one" "/" "+" "all" "some" "who" "what" "where" "when" "out" "d." "s." "so" "..."
                  "n" "i" "its" "asl" "two" "three" "new"
                  })

(defn clean-text [^String text]
  (let [
         lower-case (string/lower-case text)
         no-html (.text (org.jsoup.Jsoup/parse lower-case))
         clean (string/replace no-html #"(\{|\||\}|=|'|#|\[|\]|`|--)+" " " )
         ]
    clean
    ))

(defn is-bad-word [^String word]
  (or (stop-words word)
    (re-matches #"^(\d+|http).*$" word)
    (.contains word ".")
    (< (count word) 3)
    ))

(defn token-stream [^String text]
  (iterator-seq (edu.stanford.nlp.process.PTBTokenizer. (java.io.StringReader. text)
                  (edu.stanford.nlp.process.CoreLabelTokenFactory.) ""))
  )

(defn word-stream [^String text]
  (for [word (map (fn [^edu.stanford.nlp.ling.CoreLabel label] (.word label))
               (iterator-seq (edu.stanford.nlp.process.PTBTokenizer. (java.io.StringReader. text)
                                                 (edu.stanford.nlp.process.CoreLabelTokenFactory.) "")))
        :when (not (is-bad-word word))
        ]
    word
    ))

(defn page-words [page] (word-stream (clean-text (article-text page))))

(defn save-counts [filename counts]
  (let [^java.io.Writer w (io/writer filename)]
    (doseq [word_count counts
           :let [^String word (get word_count 0) count (get word_count 1)]
           ]

      (.write w (str count " " word "\n"))
    )
  )
)

(defn write-word-counts [input-path output-path]
  (save-counts output-path (sort-by #(- (get % 1)) (seq-counts (apply concat (pmap page-words (wiki-dump-pages input-path)))))))

;(defn write-wiki-words [wiki-file out-file]
;  (with-open [^java.io.Writer w (io/writer out-file)]
;    (dorun
;      (for [pw (page-words (bzip2-reader wiki-file))]
;        (do
;          (.write w (clojure.string/join "\n" pw))
;          (.write w "\n")
;          ))
;      )
;    )
;  )
