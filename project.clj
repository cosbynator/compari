(defproject lda "0.1.0"
  :description "Latent Dirichlet Allocation + Topic Sensitive PageRank for Wikipedia"
  :plugins [[lein-protobuf "0.3.1"]]
  :java-source-paths ["src/java"]
  :dependencies [
                  [org.postgresql/postgresql "9.2-1003-jdbc4"]
                  [intervox/clj-progress "0.1.1"]
                  [cc.mallet/mallet "2.0.7"]
                  [instaparse "1.2.6"]
                  [com.google.protobuf/protobuf-java "2.4.1"]
                  [org.flatland/protobuf "0.7.1"]
                  [com.google.guava/guava "15.0"]
                  [org.apache.lucene/lucene-xercesImpl "3.5.0"]
                  [org.clojure/clojure "1.5.1"]
                  [org.clojure/data.xml "0.0.7"]
                  [org.clojars.achim/multiset "0.1.0-SNAPSHOT"]
                  [org.apache.commons/commons-compress "1.5"]
                  [org.apache.commons/commons-lang3 "3.1"]
                  [org.apache.logging.log4j/log4j-core "2.0-beta9"]
                  [org.apache.logging.log4j/log4j-api "2.0-beta9"]
                  [org.jsoup/jsoup "1.7.2"]
                  [edu.stanford.nlp/stanford-corenlp "3.2.0" :exclusions [xerces/xercesImpl]]
                  ])
