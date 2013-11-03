package com.thomasdimson.wikipedia.lda.java;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Queues;
import com.thomasdimson.wikipedia.Data;

import java.sql.*;
import java.util.*;

import static com.thomasdimson.wikipedia.Data.TSPRGraphNode;

public class DBAccess {
    private static final String URL = "jdbc:postgresql://localhost/wikirank";
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";
    private static final int INSERT_BATCH_SIZE = 1000;
    private static final int DEFAULT_CURSOR_SIZE = 100;

    private final Connection conn;

    public DBAccess() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", PASSWORD);
        conn = DriverManager.getConnection(URL, props);
    }

    public TSPRGraphNode findArticle(String title) throws SQLException {
        PreparedStatement st = conn.prepareStatement("SELECT * FROM articles WHERE title=? LIMIT 1");
        st.setString(1, title);
        ResultSet rs = st.executeQuery();
        try {
            if(rs.next()) {
                return row2obj(rs);
            }
        } finally {
            rs.close();
            st.close();
        }

        return null;
    }

    public boolean infoboxExists(String infobox) throws SQLException {
        PreparedStatement st = conn.prepareStatement("SELECT 1 FROM articles WHERE infobox=? LIMIT 1");
        st.setString(1, infobox);
        ResultSet rs = st.executeQuery();
        try {
            if(rs.next()) {
                return true;
            }
        } finally {
            rs.close();
            st.close();
        }
        return false;
    }

    private static class SimTuple implements Comparable<SimTuple> {
        public final TSPRGraphNode node;
        public final double sim;
        public SimTuple(TSPRGraphNode node, double sim) {
            this.node = node;
            this.sim = sim;
        }

        @Override
        public int compareTo(SimTuple o) {
            return Double.compare(sim, o.sim);
        }
    }

    public List<TSPRGraphNode> nearestNeighborsTSPR(TSPRGraphNode source, int limit) throws SQLException {
        double sourceNorm = SimilarityUtils.norm(source.getTsprList());

        PreparedStatement st = conn.prepareStatement("SELECT * FROM articles"); // ugh
        st.setFetchSize(10000);
        PriorityQueue<SimTuple> q = Queues.newPriorityQueue();
                ResultSet rs = st.executeQuery();
        try {
            while(rs.next()) {
                TSPRGraphNode other = row2obj(rs);
                double otherNorm = SimilarityUtils.norm(other.getTsprList());
                double cosine = SimilarityUtils.cosine(SimilarityUtils.dot(source.getTsprList(),
                                                       other.getTsprList()), sourceNorm, otherNorm);
                SimTuple t = new SimTuple(other, cosine);
                if(q.size() > limit && q.peek().sim < t.sim) {
                    q.remove();
                    q.add(t);
                }
            }
        } finally {
            rs.close();
            st.close();
        }

        List<TSPRGraphNode> ret = Lists.newArrayListWithCapacity(q.size());
        for(SimTuple t : q) {
            ret.add(t.node);
        }
        return ret;
    }

    public List<TSPRGraphNode> topByTSPRWithInfobox(int topic, String infobox, int limit) throws SQLException {
        PreparedStatement st = conn.prepareStatement("SELECT * FROM articles WHERE infobox=? ORDER BY tspr[?] LIMIT ?");
        st.setFetchSize(DEFAULT_CURSOR_SIZE);
        try {
            st.setString(1, infobox);
            st.setInt(2, topic);
            st.setInt(3, limit);
            return listQuery(st);
        } finally {
            st.close();
        }
    }

    public List<TSPRGraphNode> topByTSPR(int topic, int limit) throws SQLException {
        PreparedStatement st = conn.prepareStatement("SELECT * FROM articles ORDER BY tspr[?] LIMIT ?");
        st.setFetchSize(DEFAULT_CURSOR_SIZE);
        try {
            st.setInt(1, topic);
            st.setInt(2, limit);
            return listQuery(st);
        } finally {
            st.close();
        }
    }


    public List<TSPRGraphNode> topByLDAWithInfobox(int topic, String infobox, int limit) throws SQLException {
        PreparedStatement st = conn.prepareStatement("SELECT * FROM articles WHERE infobox=? ORDER BY lda[?] LIMIT ?");
        st.setFetchSize(DEFAULT_CURSOR_SIZE);
        try {
            st.setString(1, infobox);
            st.setInt(2, topic);
            st.setInt(3, limit);
            return listQuery(st);
        } finally {
            st.close();
        }
    }

    public List<TSPRGraphNode> topByLDA(int topic, int limit) throws SQLException {
        PreparedStatement st = conn.prepareStatement("SELECT * FROM articles ORDER BY lda[?] LIMIT ?");
        st.setFetchSize(DEFAULT_CURSOR_SIZE);
        try {
            st.setInt(1, topic);
            st.setInt(2, limit);
            return listQuery(st);
        } finally {
            st.close();
        }
    }

    private List<TSPRGraphNode> listQuery(PreparedStatement st) throws SQLException {
        List<TSPRGraphNode> ret = Lists.newArrayList();

        ResultSet rs = st.executeQuery();
        try {
            while(rs.next()) {
                ret.add(row2obj(rs));
            }
        } finally {
            rs.close();
        }

        return ret;
    }

    public void insertTSPRGraphNodes(Iterator<TSPRGraphNode> nodes) throws SQLException {
        while(nodes.hasNext()) {
            PreparedStatement st = conn.prepareStatement("INSERT INTO articles(id, title, infobox, lda, tspr) VALUES (?,?,?,?,?)");
            try {
                conn.setAutoCommit(false);
                for(int i = 0; i < INSERT_BATCH_SIZE && nodes.hasNext(); i++) {
                    TSPRGraphNode next = nodes.next();

                    Array ldaArray = conn.createArrayOf("float8", next.getLdaList().toArray());
                    Array tsprArray = conn.createArrayOf("float8", next.getTsprList().toArray());

                    st.setLong(1, next.getId());
                    st.setString(2, next.getTitle());
                    st.setString(3, next.getInfoboxType());
                    st.setArray(4, ldaArray);
                    st.setArray(5, tsprArray);
                    st.addBatch();
                }
                st.executeBatch();
                conn.commit();
            } finally {
                conn.setAutoCommit(true);
                st.close();
            }
        }
    }

    private TSPRGraphNode row2obj(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String title = rs.getString("title");
        String infobox = rs.getString("infobox");
        Double[] lda = (Double[])rs.getArray("lda").getArray();
        Double[] tspr = (Double[])rs.getArray("tspr").getArray();
        return TSPRGraphNode.newBuilder()
                .setId(id)
                .setTitle(title)
                .setInfoboxType(infobox)
                .addAllTspr(Arrays.asList(lda))
                .addAllTspr(Arrays.asList(tspr))
                .build();
    }
}
