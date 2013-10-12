package com.thomasdimson.wikipedia.lda.java;

import com.thomasdimson.wikipedia.Data;

public class IntermediateTSPRNode {
    public final int linearId;
    public final long id;
    public final String title;
    public final long[]edges;

    public final double[] lda;
    public final double[] tspr;

    public IntermediateTSPRNode(int linearId, long id, String title, long []edges, double[] lda) {
        this.linearId = linearId;
        this.id = id;
        this.title = title;
        this.edges = edges;
        this.lda = lda;
        this.tspr = new double[this.lda.length];
    }

    public Data.TSPRGraphNode toProto() {
        Data.TSPRGraphNode.Builder builder = Data.TSPRGraphNode.newBuilder()
                .setId(this.id)
                .setTitle(this.title);
        for(int i = 0; i < this.lda.length; i++) {
            builder.addLda(lda[i]);
            builder.addTspr(tspr[i]);
        }
        return builder.build();
    }
}
