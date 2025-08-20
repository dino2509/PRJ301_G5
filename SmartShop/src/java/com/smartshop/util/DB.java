package com.smartshop.util;

import java.sql.*;

public class DB {
    private static String url;
    private static String user;
    private static String pass;

    public static void configure(String jdbcUrl, String username, String password) {
        url = jdbcUrl; user = username; pass = password;
    }

    public static Connection getConnection() throws SQLException {
        if (url == null) throw new SQLException("DB not configured");
        return DriverManager.getConnection(url, user, pass);
    }
}
