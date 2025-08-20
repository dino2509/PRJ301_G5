package com.smartshop.util;

import jakarta.servlet.ServletContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DB {
    private static volatile boolean ready = false;
    private static String URL, USER, PASS;

    private DB() {}

    public static synchronized void initFromContext(ServletContext ctx) {
        // Đọc từ <context-param> trong web.xml
        String url  = ctx.getInitParameter("JDBC_URL");
        String user = ctx.getInitParameter("JDBC_USER");
        String pass = ctx.getInitParameter("JDBC_PASS");

        if (isBlank(url) || isBlank(user) || isBlank(pass)) {
            ready = false;
            return;
        }
        URL = url; USER = user; PASS = pass;

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ignore) {}
        ready = true;
    }

    public static boolean isReady() { return ready; }

    public static Connection getConnection() throws SQLException {
        if (!ready) throw new SQLException("DB not configured");
        return DriverManager.getConnection(URL, USER, PASS);
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
