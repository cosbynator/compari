package com.thomasdimson.wikipedia.lda.java;

import com.google.common.base.Joiner;
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

    public int determineTopicIndex(List<String> topics) throws SQLException {
        if(topics.size() == 0) {
            return 200;
        }


        double []sums = new double[201];

        for(String topic : topics) {
            System.out.println("'" + topic + "'");
            TSPRGraphNode article = findArticle(topic);
            if(article == null ){
                continue;
            }

            for(int i = 0; i < article.getLdaCount(); i++) {
                sums[i] += article.getLda(i);
            }
        }

        double maxTopicSum = 0;
        int maxTopic = 200;
        for(int i = 0; i < sums.length - 1; i++) {
            if(sums[i] > maxTopicSum) {
                maxTopicSum = sums[i];
                maxTopic = i;
            }
        }
        return maxTopic;
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


    public List<TSPRGraphNode> topByTSPRWithInfobox(int topic, String infobox, int limit) throws SQLException {
        PreparedStatement st = conn.prepareStatement("SELECT * FROM articles WHERE infobox=? ORDER BY tspr[?] DESC LIMIT ?");
        st.setFetchSize(DEFAULT_CURSOR_SIZE);
        try {
            st.setString(1, infobox);
            st.setInt(2, topic + 1);
            st.setInt(3, limit);
            return listQuery(st);
        } finally {
            st.close();
        }
    }

    public List<TSPRGraphNode> topByTSPR(int topic, int limit) throws SQLException {
        PreparedStatement st = conn.prepareStatement("SELECT * FROM articles ORDER BY tspr[?] DESC LIMIT ?");
        st.setFetchSize(DEFAULT_CURSOR_SIZE);
        try {
            st.setInt(1, topic + 1);
            st.setInt(2, limit);
            return listQuery(st);
        } finally {
            st.close();
        }
    }


    public List<TSPRGraphNode> topByLDAWithInfobox(int topic, String infobox, int limit) throws SQLException {
        PreparedStatement st = conn.prepareStatement("SELECT * FROM articles WHERE infobox=? ORDER BY lda[?] DESC LIMIT ?");
        st.setFetchSize(DEFAULT_CURSOR_SIZE);
        try {
            st.setString(1, infobox);
            st.setInt(2, topic + 1);
            st.setInt(3, limit);
            return listQuery(st);
        } finally {
            st.close();
        }
    }

    public List<TSPRGraphNode> topByLDA(int topic, int limit) throws SQLException {
        PreparedStatement st = conn.prepareStatement("SELECT * FROM articles ORDER BY lda[?] DESC LIMIT ?");
        st.setFetchSize(DEFAULT_CURSOR_SIZE);
        try {
            st.setInt(1, topic + 1);
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
                .addAllLda(Arrays.asList(lda))
                .addAllTspr(Arrays.asList(tspr))
                .build();
    }
}
