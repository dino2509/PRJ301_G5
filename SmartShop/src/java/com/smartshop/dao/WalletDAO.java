package com.smartshop.dao;

import com.smartshop.model.WalletTx;
import com.smartshop.util.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WalletDAO {

    public BigDecimal getBalance(int userId) {
        String sql = "SELECT wallet_balance FROM dbo.Users WHERE id=?";
        try (Connection cn = DB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
                return BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<WalletTx> listTx(int userId, int limit) {
        String sql = "SELECT TOP (?) id, user_id, type, amount, status, ref_order_id, created_at " +
                     "FROM dbo.WalletTransactions WHERE user_id=? ORDER BY id DESC";
        List<WalletTx> list = new ArrayList<>();
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WalletTx t = new WalletTx();
                    t.setId(rs.getInt("id"));
                    t.setUserId(rs.getInt("user_id"));
                    t.setType(rs.getString("type"));
                    t.setAmount(rs.getBigDecimal("amount"));
                    t.setStatus(rs.getString("status"));
                    int oid = rs.getInt("ref_order_id");
                    t.setRefOrderId(rs.wasNull() ? null : oid);
                    Timestamp ts = rs.getTimestamp("created_at");
                    t.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
                    list.add(t);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Create a topup request in PENDING status. */
    public int createTopupRequest(int userId, BigDecimal amount) {
        String ensureWallet = "MERGE dbo.Wallets AS t USING (SELECT ? AS user_id) AS s " +
                "ON t.user_id=s.user_id WHEN NOT MATCHED THEN INSERT(user_id,balance) VALUES(s.user_id,0);";
        String insertTx = "INSERT INTO dbo.WalletTransactions(user_id,type,amount,status) VALUES(?, 'TOPUP', ?, 'PENDING');";
        try (Connection c = DB.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps1 = c.prepareStatement(ensureWallet)) {
                ps1.setInt(1, userId);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = c.prepareStatement(insertTx, Statement.RETURN_GENERATED_KEYS)) {
                ps2.setInt(1, userId);
                ps2.setBigDecimal(2, amount);
                ps2.executeUpdate();
                try (ResultSet rs = ps2.getGeneratedKeys()) {
                    c.commit();
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Admin approve topup: move from PENDING -> APPROVED and increase balance. */
    public void approveTopup(int txId) {
        String select = "SELECT user_id, amount, status FROM dbo.WalletTransactions WHERE id=? WITH (UPDLOCK, HOLDLOCK)";
        String updTx  = "UPDATE dbo.WalletTransactions SET status='APPROVED' WHERE id=?";
        String upsertWallet = "MERGE dbo.Wallets AS t USING (SELECT ? AS user_id) AS s " +
                "ON t.user_id=s.user_id WHEN MATCHED THEN UPDATE SET balance = t.balance + ? , updated_at=SYSUTCDATETIME() " +
                "WHEN NOT MATCHED THEN INSERT(user_id,balance) VALUES(s.user_id, ?);";
        try (Connection c = DB.getConnection()) {
            c.setAutoCommit(false);
            int userId; BigDecimal amount; String status;
            try (PreparedStatement ps = c.prepareStatement(select)) {
                ps.setInt(1, txId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Topup not found");
                    userId = rs.getInt("user_id");
                    amount = rs.getBigDecimal("amount");
                    status = rs.getString("status");
                    if (!"PENDING".equals(status)) {
                        c.rollback();
                        return;
                    }
                }
            }
            try (PreparedStatement p1 = c.prepareStatement(updTx)) {
                p1.setInt(1, txId);
                p1.executeUpdate();
            }
            try (PreparedStatement p2 = c.prepareStatement(upsertWallet)) {
                p2.setInt(1, userId);
                p2.setBigDecimal(2, amount);
                p2.setBigDecimal(3, amount);
                p2.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Pay order by wallet. Throws if insufficient funds. Idempotent: if already debited for order, no double charge. */
    public void payOrderWithWallet(int userId, int orderId) {
        String selOrder = "SELECT total_amount, status FROM dbo.Orders WHERE id=? AND user_id=? FOR UPDATE";
        String selExisting = "SELECT 1 FROM dbo.WalletTransactions WHERE user_id=? AND ref_order_id=? AND type='DEBIT' AND status='APPROVED'";
        String balSql = "SELECT balance FROM dbo.Wallets WITH (UPDLOCK, HOLDLOCK) WHERE user_id=?";
        String insertTx = "INSERT INTO dbo.WalletTransactions(user_id,type,amount,status,ref_order_id) VALUES(?, 'DEBIT', ?, 'APPROVED', ?)";
        String updateBal = "UPDATE dbo.Wallets SET balance = balance - ?, updated_at=SYSUTCDATETIME() WHERE user_id=?";
        String setPaid = "UPDATE dbo.Orders SET status='PAID' WHERE id=?";
        String insPay = "INSERT INTO dbo.Payments(order_id,provider,txn_id,amount,status,raw_payload) VALUES(?, 'WALLET', ?, ?, 'PAID', NULL)";
        try (Connection c = DB.getConnection()) {
            c.setAutoCommit(false);
            java.math.BigDecimal amount;
            try (PreparedStatement ps = c.prepareStatement(selOrder)) {
                ps.setInt(1, orderId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Order not found");
                    String st = rs.getString("status");
                    amount = rs.getBigDecimal("total_amount");
                    if ("PAID".equalsIgnoreCase(st)) { c.commit(); return; }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(selExisting)) {
                ps.setInt(1, userId);
                ps.setInt(2, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { // already charged
                        try (PreparedStatement psp = c.prepareStatement(setPaid)) {
                            psp.setInt(1, orderId);
                            psp.executeUpdate();
                        }
                        c.commit();
                        return;
                    }
                }
            }
            java.math.BigDecimal balance = java.math.BigDecimal.ZERO;
            try (PreparedStatement ps = c.prepareStatement(balSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) balance = rs.getBigDecimal(1);
                }
            }
            if (balance.compareTo(amount) < 0)
                throw new RuntimeException("Insufficient wallet balance");

            try (PreparedStatement p1 = c.prepareStatement(insertTx)) {
                p1.setInt(1, userId);
                p1.setBigDecimal(2, amount);
                p1.setInt(3, orderId);
                p1.executeUpdate();
            }
            try (PreparedStatement p2 = c.prepareStatement(updateBal)) {
                p2.setBigDecimal(1, amount);
                p2.setInt(2, userId);
                p2.executeUpdate();
            }
            try (PreparedStatement p3 = c.prepareStatement(setPaid)) {
                p3.setInt(1, orderId);
                p3.executeUpdate();
            }
            try (PreparedStatement p4 = c.prepareStatement(insPay)) {
                p4.setInt(1, orderId);
                p4.setString(2, "WALLET#" + orderId + "-" + System.currentTimeMillis());
                p4.setBigDecimal(3, amount);
                p4.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
        public void topup(int userId, BigDecimal amount, String description) {
        String up = "UPDATE dbo.Users SET wallet_balance = wallet_balance + ? WHERE id=?";
        String tx = "INSERT INTO dbo.WalletTransactions(user_id, type, amount, description) VALUES(?, 'TOPUP', ?, ?)";
        try (Connection cn = DB.getConnection()) {
            cn.setAutoCommit(false);
            try (PreparedStatement ps1 = cn.prepareStatement(up);
                 PreparedStatement ps2 = cn.prepareStatement(tx)) {
                ps1.setBigDecimal(1, amount);
                ps1.setInt(2, userId);
                ps1.executeUpdate();

                ps2.setInt(1, userId);
                ps2.setBigDecimal(2, amount);
                ps2.setString(3, description);
                ps2.executeUpdate();

                cn.commit();
            } catch (SQLException ex) {
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean payWithWallet(int userId, BigDecimal amount, String description) {
        String check = "SELECT wallet_balance FROM dbo.Users WITH (UPDLOCK, ROWLOCK) WHERE id=?";
        String dec = "UPDATE dbo.Users SET wallet_balance = wallet_balance - ? WHERE id=?";
        String tx  = "INSERT INTO dbo.WalletTransactions(user_id, type, amount, description) VALUES(?, 'PAYMENT', ?, ?)";
        try (Connection cn = DB.getConnection()) {
            cn.setAutoCommit(false);
            try (PreparedStatement psC = cn.prepareStatement(check)) {
                psC.setInt(1, userId);
                BigDecimal bal;
                try (ResultSet rs = psC.executeQuery()) {
                    if (!rs.next()) { cn.rollback(); return false; }
                    bal = rs.getBigDecimal(1);
                }
                if (bal.compareTo(amount) < 0) { cn.rollback(); return false; }

                try (PreparedStatement psD = cn.prepareStatement(dec);
                     PreparedStatement psT = cn.prepareStatement(tx)) {
                    psD.setBigDecimal(1, amount);
                    psD.setInt(2, userId);
                    psD.executeUpdate();

                    psT.setInt(1, userId);
                    psT.setBigDecimal(2, amount);
                    psT.setString(3, description);
                    psT.executeUpdate();

                    cn.commit();
                    return true;
                }
            } catch (SQLException ex) {
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
