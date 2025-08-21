package com.smartshop.dao;

import java.sql.*;

public class ReviewDAO {
    // Thay thế nội dung upsert() của bạn bằng hàm dưới (hoặc chỉ thay chuỗi SQL)
    public void upsert(int productId, int userId, int rating, String comment) {
        final String SQL =
            "MERGE dbo.Reviews AS T " +
            "USING (SELECT ? AS product_id, ? AS user_id) AS S " +
            "ON (T.product_id = S.product_id AND T.user_id = S.user_id) " +
            "WHEN MATCHED THEN " +
            "  UPDATE SET rating = ?, comment = ?, updated_at = SYSDATETIME() " +
            "WHEN NOT MATCHED THEN " +
            "  INSERT (product_id, user_id, rating, comment, created_at, updated_at) " +
            "  VALUES (S.product_id, S.user_id, ?, ?, SYSDATETIME(), SYSDATETIME());"; // ← dấu ; bắt buộc

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL)) {
            ps.setInt(1, productId);
            ps.setInt(2, userId);
            ps.setInt(3, rating);
            ps.setString(4, comment);
            ps.setInt(5, rating);
            ps.setString(6, comment);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Điều chỉnh cho phù hợp dự án của bạn: JNDI hoặc DriverManager
    private Connection getConnection() throws SQLException {
        // Ví dụ đơn giản dùng DriverManager. Đổi URL/User/Pass cho đúng cấu hình của bạn.
        // Khuyến nghị: dùng DataSource/JNDI trong thực tế.
        String url = "jdbc:sqlserver://localhost:1433;databaseName=SmartShop;encrypt=false";
        String user = "sa";
        String pass = "sa";
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ignore) {}
        return DriverManager.getConnection(url, user, pass);
    }
}
