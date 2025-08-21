package com.smartshop.dao;

import com.smartshop.util.DB;

import java.math.BigDecimal;
import java.sql.*;

public class PaymentDAO {

    public void recordPaymentAndMarkPaid(int orderId, String provider, String txnId, BigDecimal amount, String rawPayload) {
        String check = "SELECT 1 FROM dbo.Payments WHERE txn_id=?";
        String ins = "INSERT INTO dbo.Payments(order_id,provider,txn_id,amount,status,raw_payload) VALUES(?,?,?,?, 'PAID', ?)";
        String setPaid = "UPDATE dbo.Orders SET status='PAID' WHERE id=?";
        try (Connection c = DB.getConnection()) {
            c.setAutoCommit(false);
            boolean exists;
            try (PreparedStatement ps = c.prepareStatement(check)) {
                ps.setString(1, txnId);
                try (ResultSet rs = ps.executeQuery()) { exists = rs.next(); }
            }
            if (!exists) {
                try (PreparedStatement ps = c.prepareStatement(ins)) {
                    ps.setInt(1, orderId);
                    ps.setString(2, provider);
                    ps.setString(3, txnId);
                    ps.setBigDecimal(4, amount);
                    ps.setString(5, rawPayload);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = c.prepareStatement(setPaid)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public java.math.BigDecimal getOrderAmount(int orderId, int userId) {
        String sql = "SELECT total_amount FROM dbo.Orders WHERE id=? AND user_id=?";
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new RuntimeException("Order not found");
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
