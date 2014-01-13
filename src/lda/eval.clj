(ns lda.eval
  (:import com.thomasdimson.wikipedia.lda.java.DBAccess)   
  (:import com.thomasdimson.wikipedia.lda.java.SimilarityUtils)   
  (:import com.thomasdimson.wikipedia.lda.java.TopicSensitivePageRank)
)
(use 'clojure.java.io)
(require ['clojure.string :as 'str])

(set! *warn-on-reflection* true)

(def gold-physicists "data/eval/top_physicists.txt") ; http://worldohistory.blogspot.com/2008/08/50-most-influential-physicistsastronome.html
(def gold-mathematicians "data/eval/top_mathematicians.txt") ; http://fabpedigree.com/james/mathmen.htm
(def gold-scifi "data/eval/top_scifi_movies.txt") ; http://scifilists.sffjazz.com/lists_film.html
(def gold-anime "data/eval/top_anime_movies.txt") ; http://www.flickchart.com/Charts.aspx?genre=Anime&perpage=100

(def anime-topic-index 158) ; Anime
(def mathematics-index 183) ; Mathematics
(def physics-index 191) ; Particle physics
(def scifi-index 30) ; Blade Runner

; Macro zone
(defmacro dbg-b [str & body] `(do (println ~str) ~@body))
(defmacro dbg-a [str & body] `(let [x# ~@body] (do (println ~str) x#)))
(defmacro dbg [& body] `(let [x# ~@body] (do (println ) x#)))
(defmacro tee [f s] (do (println s) (.write f s) (.flush f)))

(defn average [coll]  (if (= (count coll) 0) 0 (/ (reduce + coll) (count coll))))

(defn mean-average-precision [candidate-list relevant-set]
  (let 
    [relevant-indices (keep-indexed (fn [index item] (when (some #{item} relevant-set) index)) candidate-list) 
     precision-at (into [] (map-indexed (fn [idx item] (/ (+ idx 1) (+ item 1))) relevant-indices))]
    (average precision-at)
  )
)

(defn article-titles [articles] (into [] (map #(.getTitle %) articles)))


(defn git [f] (TopicSensitivePageRank/newTSPRGraphNodeIterator f))
(defn evaluate-file [file-name data-file infobox topic-index w] 
  (let [articles (str/split-lines (slurp file-name))
        limit 100
        eval-map (fn [candidates] (double (mean-average-precision (article-titles candidates) articles)))
       ]
    (do
      (tee w (str "\tTSPR MAP: " (eval-map (SimilarityUtils/topByTSPRWithInfobox (git data-file) topic-index infobox limit))))
      (tee w (str "\tLSPR MAP: " (eval-map (SimilarityUtils/topByLSPRWithInfobox (git data-file) topic-index infobox limit))))
      (tee w (str "\tLDA MAP: " (eval-map (SimilarityUtils/topByLDAWithInfobox (git data-file) topic-index infobox limit))))
      (tee w (str "\tEMass MAP: " (eval-map (SimilarityUtils/topByExpectedMassWithInfobox (git data-file) topic-index infobox limit))))
    )
  ))

(defn evaluate-all [evaluation-output-file]
  (with-open [w evaluation-output-file]
    (do
      (tee w "Top Anime Films - Document Probability")
      (evaluate-file "data/eval/top_anime_movies.txt" "data/tspr_lspr.dat" "film" anime-topic-index w)
      (tee w "Top Anime Films - Likelihood")
      (evaluate-file "data/eval/top_anime_movies.txt" "data/tspr_lspr_likelihood.dat" "film" anime-topic-index w)

      (tee w "Top Mathematicians - Document Probability")
      (evaluate-file "data/eval/top_mathematicians.txt" "data/tspr_lspr.dat" "scientist" mathematics-index w)
      (tee w "Top Mathematicians - Likelihood")
      (evaluate-file "data/eval/top_mathematicians.txt" "data/tspr_lspr_likelihood.dat" "scientist" mathematics-index w)

      (tee w "Top Physicists - Document Probability")
      (evaluate-file "data/eval/top_mathematicians.txt" "data/tspr_lspr.dat" "scientist" physics-index w)
      (tee w "Top Physicists - Likelihood")
      (evaluate-file "data/eval/top_mathematicians.txt" "data/tspr_lspr_likelihood.dat" "scientist" physics-index w)

      (tee w "Top Sci-Fi Films - Document Probability")
      (evaluate-file "data/eval/top_scifi_movies.txt" "data/tspr_lspr.dat" "film" scifi-index w)
      (tee w "Top Sci-Fi Films - Likelihood")
      (evaluate-file "data/eval/top_scifi_movies.txt" "data/tspr_lspr_likelihood.dat" "film" scifi-index w)
    )  
  )
)
