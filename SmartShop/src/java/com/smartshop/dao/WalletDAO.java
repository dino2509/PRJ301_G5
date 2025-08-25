package com.smartshop.dao;

import com.smartshop.model.WalletTx;
import com.smartshop.util.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * WalletDAO compatible with existing code paths.
 * Uses snake_case columns: user_id, balance, updated_at in table Wallets,
 * and user_id, amount, type, ref_id, related_id, note, created_at in WalletTransactions.
 * Creates missing tables if needed. Does not rename existing columns.
 */
public class WalletDAO implements AutoCloseable {
    private final Connection con;
    private final boolean own;

    public WalletDAO(Connection con){ this.con = con; this.own = false; }
    public WalletDAO() throws SQLException { this.con = com.smartshop.util.DB.getConnection(); this.own = true; }
    @Override public void close(){ if (own) try{ con.close(); }catch(Exception ignore){} }

    private void ensureTables() throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("""
              IF OBJECT_ID('dbo.Wallets','U') IS NULL
              BEGIN
                CREATE TABLE dbo.Wallets(
                  user_id INT PRIMARY KEY,
                  balance DECIMAL(19,2) NOT NULL CONSTRAINT DF_Wallets_balance DEFAULT(0),
                  updated_at DATETIME2 NOT NULL CONSTRAINT DF_Wallets_updated_at DEFAULT SYSUTCDATETIME()
                );
              END
            """);
            st.execute("""
              IF OBJECT_ID('dbo.WalletTransactions','U') IS NULL
              BEGIN
                CREATE TABLE dbo.WalletTransactions(
                  id BIGINT IDENTITY(1,1) PRIMARY KEY,
                  user_id INT NOT NULL,
                  amount DECIMAL(19,2) NOT NULL,
                  type NVARCHAR(20) NULL,
                  note NVARCHAR(255) NULL,
                  created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
                );
              END
            """);
        }
    }

    public void ensureWallet(int userId) throws SQLException {
        ensureTables();
        try (PreparedStatement ps = con.prepareStatement("""
            IF NOT EXISTS(SELECT 1 FROM dbo.Wallets WHERE user_id=?)
            BEGIN
              INSERT INTO dbo.Wallets(user_id, balance) VALUES(?, 0);
            END
        """)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public BigDecimal getBalance(int userId) throws SQLException {
        ensureWallet(userId);
        try (PreparedStatement ps = con.prepareStatement("SELECT balance FROM dbo.Wallets WHERE user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next()? rs.getBigDecimal(1) : BigDecimal.ZERO; }
        }
    }

    public void addBalance(int userId, BigDecimal amount, Long refId, String note) throws SQLException {
        addBalance(userId, amount, refId, note, (refId==null?"ADJUST":"TOPUP"));
    }

    public void addBalance(int userId, BigDecimal amount, Long refId, String note, String type) throws SQLException {
        ensureWallet(userId);
        boolean prev = con.getAutoCommit();
        con.setAutoCommit(false);
        try (PreparedStatement up = con.prepareStatement(
                 "UPDATE dbo.Wallets SET balance=balance+?, updated_at=SYSUTCDATETIME() WHERE user_id=?")) {
            up.setBigDecimal(1, amount);
            up.setInt(2, userId);
            if (up.executeUpdate()!=1) throw new SQLException("Wallet row missing");

            final String table = "dbo.WalletTransactions";
            boolean hasType   = hasColumn(table, "type");
            boolean hasRef    = hasColumn(table, "ref_id");
            boolean hasRel    = hasColumn(table, "related_id");
            boolean hasNote   = hasColumn(table, "note");
            boolean hasStatus = hasColumn(table, "status");
            boolean statusNN  = hasStatus && isNotNullable(table, "status"); // cần giá trị

            // Xây câu lệnh INSERT theo cột thực tế
            StringBuilder cols = new StringBuilder("user_id,amount");
            StringBuilder qs   = new StringBuilder("?,?");
            if (hasType)   { cols.append(",type");   qs.append(",?"); }
            if (hasRef)    { cols.append(",ref_id"); qs.append(",?"); }
            if (hasRel)    { cols.append(",related_id"); qs.append(",?"); }
            if (hasNote)   { cols.append(",note");   qs.append(",?"); }
            if (hasStatus) { cols.append(",status"); qs.append(",?"); }

            String sql = "INSERT INTO "+table+"("+cols+") VALUES ("+qs+")";
            try (PreparedStatement ins = con.prepareStatement(sql)) {
                int i=1;
                ins.setInt(i++, userId);
                ins.setBigDecimal(i++, amount);
                if (hasType)   ins.setString(i++, type);
                if (hasRef)    { if (refId==null) ins.setNull(i++, Types.BIGINT); else ins.setLong(i++, refId); }
                if (hasRel)    ins.setNull(i++, Types.BIGINT);
                if (hasNote)   ins.setString(i++, note);
                if (hasStatus) ins.setString(i++, statusNN ? "SUCCESS" : "SUCCESS"); // luôn set "SUCCESS"
                ins.executeUpdate();
            }

            con.commit();
        } catch(SQLException ex){
            con.rollback();
            throw ex;
        } finally { con.setAutoCommit(prev); }
    }

    private boolean hasColumn(String table, String col) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(?) AND name = ?")) {
            ps.setString(1, table);
            ps.setString(2, col);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private boolean isNotNullable(String table, String col) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT is_nullable FROM sys.columns WHERE object_id = OBJECT_ID(?) AND name = ?")) {
            ps.setString(1, table);
            ps.setString(2, col);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1)==0;
                return false;
            }
        }
    }

    /** Fetch recent transactions for a user. */
    public List<WalletTx> findRecentTx(int userId, int limit) {
        List<WalletTx> list = new ArrayList<>();
        if (limit <= 0) limit = 50;
        try (PreparedStatement ps = con.prepareStatement("""
                SELECT TOP (?) id, user_id, amount, type, ref_id, related_id, note, created_at
                FROM dbo.WalletTransactions
                WHERE user_id=?
                ORDER BY id DESC
            """)) {
            ps.setInt(1, limit);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WalletTx t = new WalletTx();
                    t.setId(rs.getInt("id"));
                    t.setUserId(rs.getInt("user_id"));
                    t.setType(rs.getString("type"));
                    t.setAmount(rs.getBigDecimal("amount"));
                    try { t.setStatus(null); } catch (Exception ignore) {}
                    int oid = rs.getInt("related_id");
                    t.setRefOrderId(rs.wasNull() ? null : oid);
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime ldt = ts == null ? null : ts.toLocalDateTime();
                    try { t.setCreatedAt(ldt); } catch (Exception ignore) {}
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
