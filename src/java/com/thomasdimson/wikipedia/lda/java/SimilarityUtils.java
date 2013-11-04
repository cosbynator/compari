package com.thomasdimson.wikipedia.lda.java;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Queues;
import com.thomasdimson.wikipedia.Data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

public class SimilarityUtils {
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

    public static double l2LDA(Data.TSPRGraphNode n1, Data.TSPRGraphNode n2) {
        return l2(n1.getLdaList(), n2.getLdaList());
    }

    public static double l2TSPR(Data.TSPRGraphNode n1, Data.TSPRGraphNode n2) {
        return l2(n1.getTsprList(), n2.getTsprList());
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
}
