package com.smartshop.dao;

import java.sql.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public class WalletTopupDAO {
    private final Connection con;
    public WalletTopupDAO(Connection con){ this.con = con; }

    public long createPending(int userId, BigDecimal amount, String otp, Instant expiresAt, String adminEmail) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO WalletTopupRequests(UserId,Amount,Otp,ExpiresAt,Status,AdminEmail) VALUES(?,?,?,?, 'PENDING', ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, otp);
            ps.setTimestamp(4, Timestamp.from(expiresAt));
            ps.setString(5, adminEmail);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()){
                if (rs.next()) return rs.getLong(1);
                throw new SQLException("No ID generated");
            }
        }
    }

    public Topup load(long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT Id,UserId,Amount,Otp,ExpiresAt,Status,Attempts,AdminEmail,CreatedAt,VerifiedAt FROM WalletTopupRequests WHERE Id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()){
                if(!rs.next()) return null;
                Topup t = new Topup();
                t.id = rs.getLong("Id");
                t.userId = rs.getInt("UserId");
                t.amount = rs.getBigDecimal("Amount");
                t.otp = rs.getString("Otp");
                t.expiresAt = rs.getTimestamp("ExpiresAt").toInstant();
                t.status = rs.getString("Status");
                t.attempts = rs.getInt("Attempts");
                t.adminEmail = rs.getString("AdminEmail");
                return t;
            }
        }
    }

    public void incAttempts(long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE WalletTopupRequests SET Attempts=Attempts+1 WHERE Id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void markSuccess(long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE WalletTopupRequests SET Status='SUCCESS', VerifiedAt=SYSUTCDATETIME() WHERE Id=? AND Status='PENDING'")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void markExpired(long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE WalletTopupRequests SET Status='EXPIRED' WHERE Id=? AND Status='PENDING'")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public static class Topup {
        public long id; public int userId; public BigDecimal amount;
        public String otp; public Instant expiresAt; public String status;
        public int attempts; public String adminEmail;
    }
}
