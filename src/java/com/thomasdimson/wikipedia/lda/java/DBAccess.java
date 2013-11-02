package com.thomasdimson.wikipedia.lda.java;

import java.sql.*;
import java.util.Properties;

public class DBAccess {
    private static final String URL = "jdbc:postgresql://localhost/wikirank";
    private static final String USER = "postgresql";
    private static final String PASSWORD = "postgresql";

    private final Connection conn;

    public DBAccess() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", PASSWORD);
        conn = DriverManager.getConnection(URL, props);
    }

    public String findArticle(String title) throws SQLException {
        PreparedStatement st = conn.prepareStatement("SELECT * FROM articles WHERE title=? LIMIT 1");
        st.setString(1, title);
        ResultSet rs = st.executeQuery();
        try {
            if(rs.next()) {
                return row2obj(rs);
            }
        } finally {
            rs.close();
        }

        return null;
    }

    private String row2obj(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String title = rs.getString("title");
        String infobox = rs.getString("infobox");
        Double[] lda = (Double[])rs.getArray("lda").getArray();
        Double[] tspr = (Double[])rs.getArray("tspr").getArray();
        return null;
    }
}
