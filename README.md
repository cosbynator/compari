# Compari

Compari is a comparison engine for named-entities.

In particular, this is a project to investigate the how Wikipedia's graph
structure can be used to assist in the discovery and ranking
of different *topics*. It will be used for my [CS 221](http://cs221.stanford.edu) 
(Artificial Intelligence) final project at Stanford.

## Usage
Poke around
* `src/java` contains code for parsing wikipedia and performing Latent Dirichlet Allocation (LDA)
* `src/lda/core.clj` contains all the stuff needed compute Topic-Sensitive PageRank on Wikipedia
* `src/query/core.clj` query engine code
* `src/query/handler.clj` web interface for query engine


## License
Copyright © 2013 Thomas Dimson. All rights reserved.
