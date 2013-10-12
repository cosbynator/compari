(ns lda.core-test
  (:require [clojure.test :refer :all]
            [lda.core :refer :all]))


(def some-wiki "<div style=\"font-size:162%; border:none; margin:0; padding:.1em; color:#000;\">Welcome to [[Wikipedia]],</div>
  <div style=\"top:+0.2em; font-size:95%;\">the [[free content|free]] [[encyclopedia]] that [[Wikipedia:Introduction|anyone can edit]].</div>")

(deftest test-textual-links
  (testing "Compute textual links from wiki markup")
  (is (= ["Wikipedia" "free" "encyclopedia" "anyone can edit"] (into [] (textual-links some-wiki))))
  )

