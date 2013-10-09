(in-ns 'lda.core)

(require '[clojure.java.io :as io])
(require '[clojure.string :as string])

(defn index-map [sequence] (apply hash-map (apply concat (map-indexed #(vector %2 %1) sequence))))

(defn read-word-mapping [filename idx]
  (index-map
    (for [line (line-seq (io/reader filename))
          :let [word (nth (string/split line #"\s+") idx nil)]
          :when (and word (not (stop-words word)))]
      word)))


(defn vowpalify-seq [idx-map text-seq]
  (str "| " (string/join " "
             (map #(str (get %1 0) ":" (get %1 1))
               (sort (seq (seq-counts (for [word text-seq :let [idx (idx-map word)] :when idx] idx))))))))

(defn write-vowpal-dump [input-path word-map-path output-path]
  (let [idx-map (read-word-mapping word-map-path 1)]
    (with-open [^java.io.Writer w (io/writer output-path)]
      (do
      (doseq [words (pmap page-words (wiki-dump-articles input-path))
              :let [vowpal (vowpalify-seq idx-map words)]]
        (do
          (.write w vowpal)
          (.write w "\n")
        )
    ))))
  )
