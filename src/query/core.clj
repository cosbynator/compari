(ns query.core
  (:use [clojure.pprint :only [pprint]])
  (:require [instaparse.core :as insta])
)

(set! *warn-on-reflection* true)

(require '[clojure.string :as string])
(require '[clojure.java.io :as io])


; Debug macros
(defmacro dbg-b [str & body] `(do (println ~str) ~@body))
(defmacro dbg-a [str & body] `(let [x# ~@body] (do (println ~str) x#)))


(def question-parse
  (insta/parser
    "
      <S> = similar_query <(<sep> '?' | '?')>

      similar_query = (<question_phrase> <sep>)? <'similar'> (<sep> <'to'>)? <sep> article_title
      question_phrase = question_word | question_word <sep> 'is'

      article_title = !'to ' #'\\w+(\\s+\\w+)*'

      question_word = 'who'|'what'|'Who'|'What'
      sep = #'\\s+'
    ")
)

(defn parse-elements [element tree]
  (filter #(and (vector? %) (= element (first %))) (tree-seq vector? seq tree)))

(defn lift-article-titles [tree] (map second (parse-elements :article_title tree)))

(defn- perform-similar-tree [sim-tree]
  (let [article-title (first (lift-article-titles sim-tree))]
    (do
      (dbg-b (str "Interpretation: finding similar articles to " article-title))
    )
  )
)

(defn perform-query [query]
  (let [parse-tree (question-parse query)]
    (case (first (first parse-tree))
      :similar_query (perform-similar-tree (first parse-tree))
     )
  )
)





