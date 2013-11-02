package com.thomasdimson.wikipedia.lda.java;

import java.util.Iterator;
import java.util.List;

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
}
