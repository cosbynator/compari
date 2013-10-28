(ns query.core
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
      S = question_word <sep> '?'
      sep = ' '+
      question_word = 'who'|'which'|'what'
    ")
)



