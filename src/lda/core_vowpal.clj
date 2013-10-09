(in-ns 'lda.core)

(require '[clojure.java.io :as io])
(require '[clojure.string :as string])
(require '[multiset.core :as ms])
(import '(com.google.common.collect ImmutableMultiset HashMultiset Multiset))

(defn index-map [sequence] (apply hash-map (apply concat (map-indexed #(vector %2 %1) sequence))))

(defn read-word-mapping [filename idx]
  (index-map (map #(nth (string/split % #"\s+") idx) (line-seq (io/reader filename)))))

(defn seq-counts [seq] (ms/multiplicities (apply ms/multiset seq)))

(defn vowpalify-seq [idx-map text-seq]
  (str "| " (string/join " "
             (map #(str (get %1 0) ":" (get %1 1))
               (sort (seq (seq-counts (map idx-map text-seq))))))))
