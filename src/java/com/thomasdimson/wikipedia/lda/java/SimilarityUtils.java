package com.thomasdimson.wikipedia.lda.java;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Queues;
import com.thomasdimson.wikipedia.Data;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.StorelessCovariance;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

public class SimilarityUtils {
    private static final int PAGERANK_INDEX = 200;
    public static double dot(List<Double> d1, List<Double> d2) {
        Iterator<Double> d1i = d1.iterator();
        Iterator<Double> d2i = d2.iterator();
        double ret = 0.0;
        while(d1i.hasNext() && d2i.hasNext()) {
            ret += d1i.next() * d2i.next();
        }
        return ret;

    }

    public static double norm(List<Double> ds) {
        double norm = 0;
        for(Double d : ds) {
            norm += d * d;
        }
        return Math.sqrt(norm);
    }

    public static double cosine(double dot, double n1, double n2) {
        return dot / (n1 * n2);
    }

    public static double l2(List<Double> ds1, List<Double> ds2) {
        Iterator<Double> i1 = ds1.iterator();
        Iterator<Double> i2 = ds2.iterator();
        double ret = 0.0;
        while(i1.hasNext() && i2.hasNext()) {
            double d1 = i1.next();
            double d2 = i2.next();
            ret += (d1 - d2) * (d1 -d2);
        }
        return Math.sqrt(ret);
    }

    public static  double cosineLDA(Data.TSPRGraphNode n1, Data.TSPRGraphNode n2) {
        return dot(n1.getLdaList(), n2.getLdaList()) / (norm(n1.getLdaList()) * norm(n2.getLdaList()));
    }

    public static double cosineTSPR(Data.TSPRGraphNode n1, Data.TSPRGraphNode n2) {
        return dot(n1.getTsprList(), n2.getTsprList()) / (norm(n1.getTsprList()) * norm(n2.getTsprList()));
    }

    public static double cosineLSPR(Data.TSPRGraphNode n1, Data.TSPRGraphNode n2) {
        return dot(n1.getLsprList(), n2.getLsprList()) / (norm(n1.getLsprList()) * norm(n2.getLsprList()));
    }

    public static double l2LDA(Data.TSPRGraphNode n1, Data.TSPRGraphNode n2) {
        return l2(n1.getLdaList(), n2.getLdaList());
    }

    public static double l2TSPR(Data.TSPRGraphNode n1, Data.TSPRGraphNode n2) {
        return l2(n1.getTsprList(), n2.getTsprList());
    }

    public static double l2LSPR(Data.TSPRGraphNode n1, Data.TSPRGraphNode n2) {
        return l2(n1.getLsprList(), n2.getLsprList());
    }

    public static List<Data.TSPRGraphNode> nearestNeighbors(final Data.TSPRGraphNode source,
                                                            Iterator<Data.TSPRGraphNode> nodes,
                                                            String features,
                                                            String metric,
                                                            int limit) throws SQLException {
        if(features.equalsIgnoreCase("lda")) {
            if(metric.equalsIgnoreCase("cosine")) {
                return SimilarityUtils.nearestNeighborsLDACosine(source, nodes, limit);
            } else if(metric.equalsIgnoreCase("l2")) {
                return SimilarityUtils.nearestNeighborsLDAL2(source, nodes, limit);
            }
        } else if (features.equalsIgnoreCase("tspr")) {
            if(metric.equalsIgnoreCase("cosine")) {
                return SimilarityUtils.nearestNeighborsTSPRCosine(source, nodes, limit);
            } else if(metric.equalsIgnoreCase("l2")) {
                return SimilarityUtils.nearestNeighborsTSPRL2(source, nodes, limit);
            }
        } else if (features.equalsIgnoreCase("lspr")) {
            if(metric.equalsIgnoreCase("cosine")) {
                return SimilarityUtils.nearestNeighborsLSPRCosine(source, nodes, limit);
            } else if(metric.equalsIgnoreCase("l2")) {
                return SimilarityUtils.nearestNeighborsLSPRL2(source, nodes, limit);
            }
        }

        throw new RuntimeException("Bad arguments " + features + "/" + metric);
    }

    public static List<Data.TSPRGraphNode> nearestNeighborsTSPRCosine(final Data.TSPRGraphNode source,
                                                                      Iterator<Data.TSPRGraphNode> nodes,
                                                         int limit) throws SQLException {
        Ordering<Data.TSPRGraphNode> byTSPRSim = new Ordering<Data.TSPRGraphNode>() {
            @Override
            public int compare(Data.TSPRGraphNode tsprGraphNode, Data.TSPRGraphNode tsprGraphNode2) {
                return Double.compare(cosineTSPR(tsprGraphNode, source), cosineTSPR(tsprGraphNode2, source));
            }
        };

        return Lists.newArrayList(byTSPRSim.greatestOf(nodes, limit));
    }

    public static List<Data.TSPRGraphNode> nearestNeighborsLSPRCosine(final Data.TSPRGraphNode source,
                                                                      Iterator<Data.TSPRGraphNode> nodes,
                                                         int limit) throws SQLException {
        Ordering<Data.TSPRGraphNode> byLSPRSim = new Ordering<Data.TSPRGraphNode>() {
            @Override
            public int compare(Data.TSPRGraphNode tsprGraphNode, Data.TSPRGraphNode tsprGraphNode2) {
                return Double.compare(cosineLSPR(tsprGraphNode, source), cosineLSPR(tsprGraphNode2, source));
            }
        };

        return Lists.newArrayList(byLSPRSim.greatestOf(nodes, limit));
    }

    public static List<Data.TSPRGraphNode> nearestNeighborsLDACosine(final Data.TSPRGraphNode source,
                                                                      Iterator<Data.TSPRGraphNode> nodes,
                                                                      int limit) throws SQLException {
        Ordering<Data.TSPRGraphNode> byTSPRSim = new Ordering<Data.TSPRGraphNode>() {
            @Override
            public int compare(Data.TSPRGraphNode tsprGraphNode, Data.TSPRGraphNode tsprGraphNode2) {
                return Double.compare(cosineLDA(tsprGraphNode, source), cosineTSPR(tsprGraphNode2, source));
            }
        };

        return Lists.newArrayList(byTSPRSim.greatestOf(nodes, limit));
    }

    public static List<Data.TSPRGraphNode> nearestNeighborsLDAL2(final Data.TSPRGraphNode source,
                                                                  Iterator<Data.TSPRGraphNode> nodes,
                                                                  int limit) throws SQLException {
        Ordering<Data.TSPRGraphNode> byTSPRSim = new Ordering<Data.TSPRGraphNode>() {
            @Override
            public int compare(Data.TSPRGraphNode tsprGraphNode, Data.TSPRGraphNode tsprGraphNode2) {
                return Double.compare(1.0 / l2LDA(tsprGraphNode, source), 1.0 / l2LDA(tsprGraphNode2, source));
            }
        };

        return Lists.newArrayList(byTSPRSim.greatestOf(nodes, limit));
    }

    public static List<Data.TSPRGraphNode> nearestNeighborsTSPRL2(final Data.TSPRGraphNode source,
                                                                      Iterator<Data.TSPRGraphNode> nodes,
                                                                      int limit) throws SQLException {
        Ordering<Data.TSPRGraphNode> byTSPRSim = new Ordering<Data.TSPRGraphNode>() {
            @Override
            public int compare(Data.TSPRGraphNode tsprGraphNode, Data.TSPRGraphNode tsprGraphNode2) {
                return Double.compare(1.0 / l2TSPR(tsprGraphNode, source), 1.0 / l2TSPR(tsprGraphNode2, source));
            }
        };

        return Lists.newArrayList(byTSPRSim.greatestOf(nodes, limit));
    }

    public static List<Data.TSPRGraphNode> nearestNeighborsLSPRL2(final Data.TSPRGraphNode source,
                                                                      Iterator<Data.TSPRGraphNode> nodes,
                                                                      int limit) throws SQLException {
        Ordering<Data.TSPRGraphNode> byLSPRSim = new Ordering<Data.TSPRGraphNode>() {
            @Override
            public int compare(Data.TSPRGraphNode tsprGraphNode, Data.TSPRGraphNode tsprGraphNode2) {
                return Double.compare(1.0 / l2LSPR(tsprGraphNode, source), 1.0 / l2LSPR(tsprGraphNode2, source));
            }
        };

        return Lists.newArrayList(byLSPRSim.greatestOf(nodes, limit));
    }

    public static Iterator<Data.TSPRGraphNode> withInfobox(Iterator<Data.TSPRGraphNode> nodes, final String infobox) {
        if(infobox == null) {
            return nodes;
        }

        return Iterators.filter(nodes, new Predicate<Data.TSPRGraphNode>() {
            @Override
            public boolean apply(Data.TSPRGraphNode tsprGraphNode) {
                return tsprGraphNode.getInfoboxType().equals(infobox);
            }
        });
    }

    public static List<Data.TSPRGraphNode> topByTSPRWithInfobox(Iterator<Data.TSPRGraphNode> nodes,
                                                         final int topic,
                                                         final String infobox,
                                                         int limit) throws SQLException {

        Ordering<Data.TSPRGraphNode> byTSPR = new Ordering<Data.TSPRGraphNode>() {
            @Override
            public int compare(Data.TSPRGraphNode tsprGraphNode, Data.TSPRGraphNode tsprGraphNode2) {
                return Double.compare(tsprGraphNode.getTspr(topic), tsprGraphNode2.getTspr(topic));
            }
        };

        return Lists.newArrayList(byTSPR.greatestOf(withInfobox(nodes, infobox), limit));
    }

    public static List<Data.TSPRGraphNode> topByLSPRWithInfobox(Iterator<Data.TSPRGraphNode> nodes,
                                                         final int topic,
                                                         final String infobox,
                                                         int limit) throws SQLException {

        Ordering<Data.TSPRGraphNode> byLSPR = new Ordering<Data.TSPRGraphNode>() {
            @Override
            public int compare(Data.TSPRGraphNode tsprGraphNode, Data.TSPRGraphNode tsprGraphNode2) {
                return Double.compare(tsprGraphNode.getLspr(topic), tsprGraphNode2.getLspr(topic));
            }
        };

        return Lists.newArrayList(byLSPR.greatestOf(withInfobox(nodes, infobox), limit));
    }

    public static List<Data.TSPRGraphNode> topByLDAWithInfobox(Iterator<Data.TSPRGraphNode> nodes,
                                                         final int topic,
                                                         final String infobox,
                                                         int limit) throws SQLException {

        Ordering<Data.TSPRGraphNode> byLDA = new Ordering<Data.TSPRGraphNode>() {
            @Override
            public int compare(Data.TSPRGraphNode tsprGraphNode, Data.TSPRGraphNode tsprGraphNode2) {
                return Double.compare(tsprGraphNode.getLda(topic), tsprGraphNode2.getLda(topic));
            }
        };

        return Lists.newArrayList(byLDA.greatestOf(withInfobox(nodes, infobox), limit));
    }

    public static List<Data.TSPRGraphNode> topByExpectedMassWithInfobox(Iterator<Data.TSPRGraphNode> nodes,
                                                        final int topic,
                                                        final String infobox,
                                                        int limit) throws SQLException {

        Ordering<Data.TSPRGraphNode> byLDA = new Ordering<Data.TSPRGraphNode>() {
            @Override
            public int compare(Data.TSPRGraphNode tsprGraphNode, Data.TSPRGraphNode tsprGraphNode2) {
                return Double.compare(tsprGraphNode.getLda(topic) * tsprGraphNode.getTspr(PAGERANK_INDEX)
                                    , tsprGraphNode2.getLda(topic) * tsprGraphNode2.getTspr(PAGERANK_INDEX));
            }
        };

        return Lists.newArrayList(byLDA.greatestOf(withInfobox(nodes, infobox), limit));
    }

    public static StorelessCovariance[] covariances(Iterator<Data.TSPRGraphNode> nodes) {
        StorelessCovariance lda = null;
        StorelessCovariance tspr = null;
        StorelessCovariance lspr = null;
        while(nodes.hasNext()) {
            Data.TSPRGraphNode node = nodes.next();
            if(lda == null) {
                lda = new StorelessCovariance(node.getLdaCount());
                tspr = new StorelessCovariance(node.getTsprCount());
                lspr = new StorelessCovariance(node.getLsprCount());
            }

            lda.increment(ArrayUtils.toPrimitive(node.getLdaList().toArray(new Double[node.getLdaCount()])));
            tspr.increment(ArrayUtils.toPrimitive(node.getTsprList().toArray(new Double[node.getTsprCount()])));
            lspr.increment(ArrayUtils.toPrimitive(node.getLsprList().toArray(new Double[node.getLsprCount()])));
        }

        return new StorelessCovariance[] {lda, tspr, lspr};
    }
}
