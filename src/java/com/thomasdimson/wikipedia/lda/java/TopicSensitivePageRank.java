package com.thomasdimson.wikipedia.lda.java;

import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.thomasdimson.wikipedia.Data;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TopicSensitivePageRank {
    public static double BETA = 0.85;
    public static int NUM_ITERATIONS = 50;

    private static Ordering<Data.TSPRGraphNode> byLDA(final int index) {
        return new Ordering<Data.TSPRGraphNode>() {
            @Override
            public int compare(Data.TSPRGraphNode tsprGraphNode, Data.TSPRGraphNode tsprGraphNode2) {
                return Double.compare(tsprGraphNode.getLda(index), tsprGraphNode2.getLda(index));
            }
        };
    }

    private static Ordering<Data.TSPRGraphNode> byTSPR(final int index) {
        return new Ordering<Data.TSPRGraphNode>() {
            @Override
            public int compare(Data.TSPRGraphNode tsprGraphNode, Data.TSPRGraphNode tsprGraphNode2) {
                return Double.compare(tsprGraphNode.getLda(index), tsprGraphNode2.getLda(index));
            }
        };
    }

    public static List<Data.TSPRGraphNode> topKLDA(Iterable<Data.TSPRGraphNode> nodes, int index, int k) {
        return byLDA(index).greatestOf(nodes, k);
    }

    public static List<Data.TSPRGraphNode> topKTSPR(Iterable<Data.TSPRGraphNode> nodes, int index, int k) {
        return byTSPR(index).greatestOf(nodes, k);
    }

    public static Iterator<Data.TSPRGraphNode> newTSPRGraphNodeIterator(String filename) throws IOException {
        final InputStream inputStream = new BufferedInputStream(new FileInputStream(filename));

        try {
            return new Iterator<Data.TSPRGraphNode>() {
                Data.TSPRGraphNode nextMessage = Data.TSPRGraphNode.parseDelimitedFrom(inputStream);

                @Override
                public boolean hasNext() {
                    return nextMessage != null;
                }

                @Override
                public Data.TSPRGraphNode next() {
                    Data.TSPRGraphNode ret = nextMessage;
                    try {
                        nextMessage = Data.TSPRGraphNode.parseDelimitedFrom(inputStream);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return ret;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void rankInPlace(List<IntermediateTSPRNode> nodes) {
        if(nodes.size() == 0) {
            return;
        }

        final int numNodes = nodes.size();
        final int numTopics = nodes.get(0).lda.length;

        Map<Long, IntermediateTSPRNode> nodeById = Maps.newHashMapWithExpectedSize(nodes.size());

        // Compute id map and topic sums
        final double []ldaSums = new double[nodes.size()];
        for(IntermediateTSPRNode node : nodes) {
            for(int j = 0; j < numTopics; j++) {
                ldaSums[j] += node.lda[j];
            }

            nodeById.put(node.id, node);
        }


        double [][] lastRank = new double[numNodes][numTopics];
        double [][] thisRank = new double[numNodes][numTopics];

        for(int iteration = 0; iteration < NUM_ITERATIONS; iteration++) {
            double [][]tmp = thisRank;
            thisRank = lastRank;
            lastRank = tmp;
            if(iteration == 0) {
                // Initialize
                for(IntermediateTSPRNode node : nodes) {
                    for(int j = 0; j < numTopics; j++) {
                        thisRank[node.linearId][j] = node.lda[j] / ldaSums[j];
                    }
                }
            } else {
                // Clear old values
                for(int i = 0; i < numNodes; i++) {
                    for(int j = 0; j < numTopics; j++) {
                        thisRank[i][j] = 0.0;
                    }
                }
            }

            // Power iteration
            for(IntermediateTSPRNode node : nodes) {
                int numNeighbors = node.edges.length;
                for(int j = 0; j < numTopics; j++) {
                    double contribution = BETA * lastRank[node.linearId][j] / numNeighbors;
                    for(long targetId : node.edges)  {
                        IntermediateTSPRNode neighbor = nodeById.get(targetId);
                        thisRank[neighbor.linearId][j] += contribution;
                    }
                }
            }

            // Reinsert leaked
            double []topicSums = new double[numTopics];
            for(IntermediateTSPRNode node : nodes) {
                for(int j = 0; j < numTopics; j++) {
                    topicSums[j] += thisRank[node.linearId][j];
                }
            }

            double difference = 0.0;
            for(IntermediateTSPRNode node : nodes) {
                for(int j = 0; j < numTopics; j++) {
                    thisRank[node.linearId][j] += (1.0 - topicSums[j]) * (node.lda[j] / ldaSums[j]);
                    // Calculate L1 difference too
                    difference += Math.abs(thisRank[node.linearId][j] - lastRank[node.linearId][j]);
                }
            }

            System.err.println("Pagerank iteration " + iteration + ": average delta=" + difference / numTopics);
        }



        for(IntermediateTSPRNode node : nodes) {
            System.arraycopy(thisRank[node.linearId], 0, node.tspr, 0, numTopics);
        }
    }
}
