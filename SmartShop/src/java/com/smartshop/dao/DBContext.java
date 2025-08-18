package com.smartshop.dao;

import java.sql.*;

public class DBContext {
    private static final String URL =
        "jdbc:sqlserver://192.168.137.1:1433;databaseName=SmartShop;encrypt=true;trustServerCertificate=true";
    private static final String USER = "sa";
    private static final String PASS = "sa";

    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Thiếu mssql-jdbc trên classpath.", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static String ping() {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM Products");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return "OK Products=" + rs.getInt(1);
        } catch (Exception e) {
            return "DB ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
