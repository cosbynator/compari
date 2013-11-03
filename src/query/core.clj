(ns query.core
  (:use [clojure.pprint :only [pprint]])
  (:require [clojure.string :as string])
  (:require [instaparse.core :as insta])
)

(set! *warn-on-reflection* true)

(require '[clojure.string :as string])
(require '[clojure.java.io :as io])


; Debug macros
(defmacro dbg-b [str & body] `(do (println ~str) ~@body))
(defmacro dbg-a [str & body] `(let [x# ~@body] (do (println ~str) x#)))
(defmacro dbg [& body] `(let [x# ~@body] (do (println x#) x#)))


(def question-parse
  (insta/parser
    "
      <S> = (similar_query|compare_query|topk_query) <query_endings>?
      query_endings = sep? ('?'|'.'|'!')

      compare_query = <'compare'> <sep> article_split (<sep> topic_refinement)?
      <article_split> = article_title <article_divider> article_title
      article_divider = sep ('and' | 'to') sep
      
      similar_query = (<sim_question_phrase> <sep>)? <'similar'> (<sep> <'to'>)? <sep> article_title
      sim_question_phrase = question_word | question_word <sep> 'is'

      topk_query = (<topk_question_phase> <sep>)? (<topk_indicator> <sep>)? topk_infobox (<sep> topic_refinement)?
      topk_infobox = word
      topk_question_phase = question_word (<sep> ('is'|'are'))?
      topk_indicator = ('the' <sep>)? ('top'|'most' <sep> 'influential'|'best'|'greatest')

      topic_refinement = <'in'> <sep> topics

      question_word = 'who'|'what'|'which'|'Who'|'What'|'Which'

      <topics> = topic (<sep>? <'/'> topic)*
      topic = word
      article_title = word (<sep> word)* |  <'\"'> word_nodblquote <'\"'> | <'\\''> word_nosglquote <'\\''>

      <word> = #'\\w+'
      <word_nodblquote> = #'[^\"]+'
      <word_nosglquote> = #'[^\\']+'
      sep  = #'\\s+'
    ")
)

; Helpers 
(defn parse-elements [element tree]
  (filter #(and (vector? %) (= element (first %))) (tree-seq vector? seq tree)))
(defn lift-article-titles [tree] (map #(string/join " " (rest %)) (parse-elements :article_title tree)))
(defn lift-topics [tree] (map #(second %) (parse-elements :topic tree)))
(defn lift-topk-infobox [tree] (map #(second %) (parse-elements :topk_infobox tree)))

; Query templates 
(defn- knn-query-template [sim-tree]
  (let [article-title (first (lift-article-titles sim-tree))]
    {
     :type :knn
     :article-title article-title
    }
))

(defn compare-template [compare-tree]
  (let [[a1 a2] (into [] (lift-article-titles compare-tree))
        topics (into [] (lift-topics compare-tree))]
    {
     :type :compare
     :first-article-title a1
     :second-article-title a2
     :topics topics
    }
  ))

(defn topk-template [topk-tree]
  (let [infobox (first (lift-topk-infobox topk-tree))
        topics (into [] (lift-topics topk-tree))]
    {
     :type :top_k
     :infobox infobox
     :topics topics
    }
))

(defn extract-query-template [query]
  (let [parse-tree (dbg (question-parse query))]
    (if (insta/failure? parse-tree)
      {:type :failure 
       :explanation (insta/get-failure parse-tree)}
      (case (first (first parse-tree))
        :similar_query (knn-query-template (first parse-tree))
        :compare_query (compare-template (first parse-tree))
        :topk_query (topk-template (first parse-tree))
        nil
      )
    )
  )
)

(defn template-explanation [query-template]
  (case (:type query-template)
    :knn (str "Similar articles to " (:article-title query-template))
  )
)
