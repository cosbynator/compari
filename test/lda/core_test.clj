(ns lda.core-test
  (:require [clojure.test :refer :all]
            [lda.core :refer :all]))

(def some-sequence ["a"  "b" "c"])

(deftest test-index-map
  (testing "FIXME, I fail."
    (is (= {"a" 0, "b" 1, "c" 2} (index-map some-sequence)))))

(deftest test-text-from-ids
  (testing "Could not get id counts from text")
    (is (= {0 3, 1 1} (seq-counts (map {"a" 0, "b" 1} ["a" "a" "b" "a"]))))
  )

(deftest test-vowpalify
  (testing "Could not vowpalify")
  (is (= "| 0:1 1:2 2:1" (vowpalify-seq {"a" 0, "b" 1, "c" 2} ["a" "b" "c" "b"])))
  )
