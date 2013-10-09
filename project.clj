(defproject lda "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [
                  ["info-bliki-repository" {
                    :url "http://gwtwiki.googlecode.com/svn/maven-repository/"
                    :snapshots true
                    :releases true
                  }]
                ]
  :dependencies [
                  [org.clojure/clojure "1.5.1"]
                  [org.clojure/data.xml "0.0.7"]
                  [org.clojars.achim/multiset "0.1.0-SNAPSHOT"]
                  [org.apache.commons/commons-compress "1.5"]
                  [org.apache.commons/commons-lang3 "3.1"]
                  [org.jsoup/jsoup "1.7.2"]
                  [edu.stanford.nlp/stanford-corenlp "3.2.0"]
                  [info.bliki.wiki/bliki-core "3.0.19"]
                  ])
