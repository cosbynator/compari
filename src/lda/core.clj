(ns lda.core)
(set! *warn-on-reflection* true)

(load "core_xml")
(load "core_vowpal")
(load "core_tokens")

(import 'com.thomasdimson.wikipedia.lda.java.MarkupCleaner)
(import 'com.thomasdimson.wikipedia.lda.java.WikipediaHandler)
(import 'com.thomasdimson.wikipedia.Data$DumpPage)


(defn clean-wiki-stream [input-file whitelist-file output-file]

  (let [^MarkupCleaner markup-cleaner (MarkupCleaner. MarkupCleaner/STOP_WORDS (MarkupCleaner/readWhitelist whitelist-file 1))
        dump-page-clean (fn [^Data$DumpPage page] [page (.cleanMarkup markup-cleaner (.getText page))])
        valid-article? (fn valid-article? [^Data$DumpPage page] (not (.hasRedirect page)))
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

