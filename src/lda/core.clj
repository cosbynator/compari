(ns lda.core)
(set! *warn-on-reflection* true)

(require '[multiset.core :as ms])

(def stop-words #{
                  "!" "," "the" "of" "and" "in" "a" "to" "-RRB-" "-LRB-" ":" "\\*" "is" "''"
                  "as" "s" "for" "by" "was" "on" "that" "with" "title" "cite" "from" "are"
                  "?" "." ";" "-" "it" "an" "or" "url" "at" "&" "his" "her" "be" "year"
                  "this" "date" "accessdate" "he" "she" "they" "were" "not" "also" "web" "%" "\\" "have"
                  "has" "one" "/" "+" "all" "some" "who" "what" "where" "when" "out" "d." "s." "so" "..."
                  "n" "i" "its" "asl" "two" "three" "new"
                  "user" "category" "talk" "utc" "align" "center"
                  "first" "link" "page" "wikipedia" "name" "publisher"
                  "which" "style" "redirect" "but" "image" "article"
                  "may" "you" "left" "time" "there" "domain" "other" "oldid"
                  "states" "flagicon" "county" "people" "had" "their" "diff"
                  "special" "coibot\\/otherlinks" "wul" "list" "can" "after"
                  "last" "work" "been" "state" "world" "made" "more"
                  "should" "would" "about" "file" "history" "links" "deletion"
                  "album" "see" "bgcolor" "into" "use" "edit" "width" "reflist"
                  "text-align" "\\*\\*" "box" "author" "used" "background" "top"
                  "most" "over" "than" "delete" "any" "yes" "these" "many" "action"
                  "him" "defaultsort" "contribs" "group" "them" "jpg"
                  })

(defn seq-counts [seq] (ms/multiplicities (apply ms/multiset seq)))

(load "core_xml")
(load "core_tokens")
(load "core_vowpal")

