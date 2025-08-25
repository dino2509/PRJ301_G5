package com.smartshop.dao;

import com.smartshop.model.WalletTx;
import com.smartshop.util.DB;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * WalletDAO compatible with existing code paths. Uses snake_case columns:
 * user_id, balance, updated_at in table Wallets, and user_id, amount, type,
 * ref_id, related_id, note, created_at in WalletTransactions. Creates missing
 * tables if needed. Does not rename existing columns.
 */
public class WalletDAO implements AutoCloseable {

    private final Connection con;
    private final boolean own;

    public WalletDAO(Connection con) {
        this.con = con;
        this.own = false;
    }

    public WalletDAO() throws SQLException {
        this.con = com.smartshop.util.DB.getConnection();
        this.own = true;
    }

    @Override
    public void close() {
        if (own) try {
            con.close();
        } catch (Exception ignore) {
        }
    }

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
              IF OBJECT_ID('dbo.TopupRequests','U') IS NULL
              BEGIN
                CREATE TABLE dbo.TopupRequests(
                  id BIGINT IDENTITY(1,1) PRIMARY KEY,
                  user_id INT NOT NULL,
                  amount DECIMAL(19,2) NOT NULL,
                  code NVARCHAR(16) NOT NULL,
                  expires_at DATETIME2 NOT NULL,
                  status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
                  created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
                );
              END
            """);
            st.execute("""
              IF OBJECT_ID('dbo.Transactions','U') IS NULL
              BEGIN
                CREATE TABLE dbo.Transactions(
                  transaction_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                  transaction_code NVARCHAR(32) NOT NULL,
                  user_id INT NOT NULL,
                  order_id BIGINT NULL,
                  type NVARCHAR(20) NOT NULL,   -- TOPUP/PURCHASE/REFUND/WITHDRAW
                  amount DECIMAL(19,2) NOT NULL,
                  method NVARCHAR(32) NOT NULL, -- WALLET/COD/GATEWAY/BANK/MOMO...
                  status NVARCHAR(20) NOT NULL, -- PENDING/SUCCESS/FAILED/CANCELLED
                  created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
                );
                CREATE UNIQUE INDEX UX_Transactions_Code ON dbo.Transactions(transaction_code);
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
                  status NVARCHAR(20) NULL,
                  created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
                );
              END
            """);
        }
    }

    private boolean tableExists(String name) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 WHERE OBJECT_ID(?, 'U') IS NOT NULL")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasColumn(String table, String col) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(?) AND name = ?")) {
            ps.setString(1, table);
            ps.setString(2, col);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /* ---------------- Wallet core ---------------- */
    public void ensureWallet(int userId) throws SQLException {
        ensureTables();
        try (PreparedStatement ps = con.prepareStatement("""
            IF NOT EXISTS(SELECT 1 FROM dbo.Wallets WHERE user_id=?)
            BEGIN INSERT INTO dbo.Wallets(user_id, balance) VALUES(?, 0); END
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
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /* ---------------- Topup 2-step ---------------- */
    public TopupRequest createTopupRequest(int userId, BigDecimal amount) throws SQLException {
        ensureWallet(userId);
        ensureTables();
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        String code = genCode();
        long id;
        try (PreparedStatement ps = con.prepareStatement("""
                INSERT INTO dbo.TopupRequests(user_id, amount, code, expires_at, status)
                VALUES (?, ?, ?, DATEADD(minute, 5, SYSUTCDATETIME()), 'PENDING');
                SELECT SCOPE_IDENTITY();
            """)) {
            ps.setInt(1, userId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, code);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        LocalDateTime exp = null;
        try (PreparedStatement ps = con.prepareStatement("SELECT expires_at FROM dbo.TopupRequests WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    exp = rs.getTimestamp(1).toLocalDateTime();
                }
            }
        }
        return new TopupRequest(id, userId, amount, code, exp);
    }

    /**
     * Xác nhận nạp. Trả về số dư mới.
     */
    public BigDecimal confirmTopup(int userId, long reqId, String code) throws SQLException {
        ensureTables();
        boolean prev = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            int updated;
            try (PreparedStatement ps = con.prepareStatement("""
                UPDATE dbo.TopupRequests
                   SET status='USED'
                 WHERE id=? AND user_id=? AND code=? AND status='PENDING' AND expires_at>SYSUTCDATETIME()
            """)) {
                ps.setLong(1, reqId);
                ps.setInt(2, userId);
                ps.setString(3, code);
                updated = ps.executeUpdate();
            }
            if (updated != 1) {
                String reason = detectTopupFailure(userId, reqId, code);
                throw new SQLException(reason);
            }

            BigDecimal amount;
            try (PreparedStatement ps = con.prepareStatement("SELECT amount FROM dbo.TopupRequests WHERE id=?")) {
                ps.setLong(1, reqId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    amount = rs.getBigDecimal(1);
                }
            }

            try (PreparedStatement up = con.prepareStatement(
                    "UPDATE dbo.Wallets SET balance=balance+?, updated_at=SYSUTCDATETIME() WHERE user_id=?")) {
                up.setBigDecimal(1, amount);
                up.setInt(2, userId);
                up.executeUpdate();
            }

            String txCode = genTxCode("TP");
            insertTransaction(userId, null, "TOPUP", amount, "WALLET", "SUCCESS", txCode);

            if (tableExists("dbo.WalletTransactions")) {
                final String tbl = "dbo.WalletTransactions";
                boolean cType = hasColumn(tbl, "type");
                boolean cNote = hasColumn(tbl, "note");
                boolean cStatus = hasColumn(tbl, "status");

                StringBuilder cols = new StringBuilder("user_id,amount");
                StringBuilder qs = new StringBuilder("?,?");
                if (cType) {
                    cols.append(",type");
                    qs.append(",?");
                }
                if (cNote) {
                    cols.append(",note");
                    qs.append(",?");
                }
                if (cStatus) {
                    cols.append(",status");
                    qs.append(",?");
                }

                String sql = "INSERT INTO " + tbl + "(" + cols + ") VALUES (" + qs + ")";
                try (PreparedStatement ins = con.prepareStatement(sql)) {
                    int i = 1;
                    ins.setInt(i++, userId);
                    ins.setBigDecimal(i++, amount);
                    if (cType) {
                        ins.setString(i++, "TOPUP");
                    }
                    if (cNote) {
                        ins.setString(i++, "Topup confirmed #" + reqId);
                    }
                    if (cStatus) {
                        ins.setString(i++, "SUCCESS");
                    }
                    ins.executeUpdate();
                }
            }

            con.commit();
            return getBalance(userId);
        } catch (SQLException ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(prev);
        }
    }

    private String detectTopupFailure(int userId, long reqId, String code) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT status, expires_at, code FROM dbo.TopupRequests WHERE id=? AND user_id=?")) {
            ps.setLong(1, reqId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "Yêu cầu không tồn tại";
                }
                String st = rs.getString("status");
                Timestamp exp = rs.getTimestamp("expires_at");
                String real = rs.getString("code");
                if (!"PENDING".equalsIgnoreCase(st)) {
                    return "Yêu cầu đã " + st;
                }
                if (exp != null && exp.toInstant().isBefore(java.time.Instant.now())) {
                    return "Mã đã hết hạn";
                }
                if (!real.equals(code)) {
                    return "Mã không đúng";
                }
            }
        }
        return "Xác nhận thất bại";
    }

    private void insertTransaction(int userId, Long orderId, String type, BigDecimal amount, String method, String status,
            String txCode) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO dbo.Transactions(transaction_code,user_id,order_id,type,amount,method,status) VALUES(?,?,?,?,?,?,?)")) {
            ps.setString(1, txCode);
            ps.setInt(2, userId);
            if (orderId == null) {
                ps.setNull(3, Types.BIGINT);
            } else {
                ps.setLong(3, orderId);
            }
            ps.setString(4, type);
            ps.setBigDecimal(5, amount);
            ps.setString(6, method);
            ps.setString(7, status);
            ps.executeUpdate();
        }
    }

    /* ---------------- Queries for history ---------------- */
    public List<TxRow> listTransactions(int userId, int limit) throws SQLException {
        ensureTables();
        List<TxRow> list = new ArrayList<>();
        if (limit <= 0) {
            limit = 50;
        }

        if (tableExists("dbo.Transactions")) {
            try (PreparedStatement ps = con.prepareStatement("""
                SELECT TOP (?) transaction_id, transaction_code, user_id, order_id, type, amount, method, status, created_at
                FROM dbo.Transactions WHERE user_id=? ORDER BY transaction_id DESC
            """)) {
                ps.setInt(1, limit);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TxRow t = new TxRow();
                        t.transactionId = rs.getLong("transaction_id");
                        t.transactionCode = rs.getString("transaction_code");
                        t.userId = rs.getInt("user_id");
                        long oid = rs.getLong("order_id");
                        t.orderId = rs.wasNull() ? null : oid;
                        t.type = rs.getString("type");
                        t.amount = rs.getBigDecimal("amount");
                        t.method = rs.getString("method");
                        t.status = rs.getString("status");
                        Timestamp ts = rs.getTimestamp("created_at");
                        t.createdAt = ts == null ? null : ts.toLocalDateTime();
                        list.add(t);
                    }
                }
            }
            return list;
        }

        try (PreparedStatement ps = con.prepareStatement("""
            SELECT TOP (?) id, user_id, amount, type, status, created_at
              FROM dbo.WalletTransactions WHERE user_id=? ORDER BY id DESC
        """)) {
            ps.setInt(1, limit);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TxRow t = new TxRow();
                    t.transactionId = rs.getLong("id");
                    t.transactionCode = "WTX-" + t.transactionId;
                    t.userId = rs.getInt("user_id");
                    t.orderId = null;
                    t.type = rs.getString("type");
                    t.amount = rs.getBigDecimal("amount");
                    t.method = "WALLET";
                    t.status = rs.getString("status");
                    Timestamp ts = rs.getTimestamp("created_at");
                    t.createdAt = ts == null ? null : ts.toLocalDateTime();
                    list.add(t);
                }
            }
        }
        return list;
    }

    /* ---------------- Helpers & DTOs ---------------- */
    private static String genCode() {
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(6);
        java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String genTxCode(String prefix) {
        String ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int rnd = java.util.concurrent.ThreadLocalRandom.current().nextInt(100, 999);
        return prefix + "-" + ts + "-" + rnd;
    }

    public static class TopupRequest {

        public final long id;
        public final int userId;
        public final BigDecimal amount;
        public final String code;
        public final LocalDateTime expiresAt;

        public TopupRequest(long id, int userId, BigDecimal amount, String code, LocalDateTime expiresAt) {
            this.id = id;
            this.userId = userId;
            this.amount = amount;
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    public static class TxRow {

        long transactionId;
        String transactionCode;
        int userId;
        Long orderId;
        String type;
        BigDecimal amount;
        String method;
        String status;
        LocalDateTime createdAt;

        public long getTransactionId() {
            return transactionId;
        }

        public String getTransactionCode() {
            return transactionCode;
        }

        public int getUserId() {
            return userId;
        }

        public Long getOrderId() {
            return orderId;
        }

        public String getType() {
            return type;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getMethod() {
            return method;
        }

        public String getStatus() {
            return status;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }

    public void addBalance(int userId, BigDecimal amount, Long refId, String note) throws SQLException {
        addBalance(userId, amount, refId, note, (refId == null ? "ADJUST" : "TOPUP"));
    }

    public void addBalance(int userId, BigDecimal delta, Long refId, String note, String type) throws SQLException {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        ensureWallet(userId);
        boolean hasRef = hasCol("dbo.WalletTransactions", "ref_id");
        boolean hasNote = hasCol("dbo.WalletTransactions", "note");
        boolean hasStatus = hasCol("dbo.WalletTransactions", "status");
        String cols = "user_id, amount, type";
        String qs = "?,?,?";
        if (hasStatus) {
            cols += ", status";
            qs += ",?";
        }
        if (hasRef) {
            cols += ", ref_id";
            qs += ",?";
        }
        if (hasNote) {
            cols += ", note";
            qs += ",?";
        }

        String sqlIns = "INSERT INTO dbo.WalletTransactions(" + cols + ") VALUES(" + qs + ")";
        try (PreparedStatement ps = con.prepareStatement(sqlIns)) {
            int i = 1;
            ps.setInt(i++, userId);
            ps.setBigDecimal(i++, delta.abs());
            ps.setString(i++, (type != null && !type.isBlank()) ? type : (delta.signum() > 0 ? "TOPUP" : "PURCHASE"));
            if (hasStatus) {
                ps.setString(i++, "SUCCESS");
            }
            if (hasRef) {
                if (refId == null) {
                    ps.setNull(i++, java.sql.Types.BIGINT);
                } else {
                    ps.setLong(i++, refId);
                }
            }
            if (hasNote) {
                ps.setString(i++, note);
            }
            ps.executeUpdate();
        }

        // Không cho âm số dư
        if (delta.signum() < 0) {
            BigDecimal bal = getBalance(userId);
            if (bal.add(delta).compareTo(BigDecimal.ZERO) < 0) {
                throw new SQLException("Insufficient funds");
            }
        }

        boolean oldAuto = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            // Update số dư (delta có thể âm khi trừ tiền)
            try (PreparedStatement up = con.prepareStatement(
                    "UPDATE dbo.Wallets SET balance = balance + ? WHERE user_id = ?")) {
                up.setBigDecimal(1, delta);
                up.setInt(2, userId);
                if (up.executeUpdate() == 0) {
                    throw new SQLException("Wallet not found");
                }
            }

            // Ghi lịch sử: amount luôn dương để qua CHECK constraint
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO dbo.WalletTransactions(user_id, amount, type, status, ref_id, note) "
                    + "VALUES (?,?,?,?,?,?)")) {
                ps.setInt(1, userId);
                ps.setBigDecimal(2, delta.abs()); // <-- quan trọng: luôn dương
                ps.setString(3, (type != null && !type.isBlank())
                        ? type
                        : (delta.signum() > 0 ? "TOPUP" : "PURCHASE"));
                ps.setString(4, "SUCCESS");
                if (refId == null) {
                    ps.setNull(5, java.sql.Types.BIGINT);
                } else {
                    ps.setLong(5, refId);
                }
                ps.setString(6, note);
                ps.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAuto);
        }
    }

    private boolean hasCol(String table, String col) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(?) AND name=?")) {
            ps.setString(1, table);
            ps.setString(2, col);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isNotNullable(String table, String col) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT is_nullable FROM sys.columns WHERE object_id = OBJECT_ID(?) AND name = ?")) {
            ps.setString(1, table);
            ps.setString(2, col);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
                return false;
            }
        }
    }

    /**
     * Fetch recent transactions for a user.
     */
    public List<WalletTx> findRecentTx(int userId, int limit) {
        List<WalletTx> list = new ArrayList<>();
        if (limit <= 0) {
            limit = 50;
        }
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
                    try {
                        t.setStatus(null);
                    } catch (Exception ignore) {
                    }
                    int oid = rs.getInt("related_id");
                    t.setRefOrderId(rs.wasNull() ? null : oid);
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime ldt = ts == null ? null : ts.toLocalDateTime();
                    try {
                        t.setCreatedAt(ldt);
                    } catch (Exception ignore) {
                    }
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
