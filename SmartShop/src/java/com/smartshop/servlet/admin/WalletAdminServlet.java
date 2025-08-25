package com.smartshop.servlet.admin;

import com.smartshop.dao.WalletDAO;
import com.smartshop.dao.WalletTopupDAO;
import com.smartshop.dao.WalletTopupDAO.Topup;
import com.smartshop.util.DB;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản trị ví: xem topup pending, duyệt/hủy, điều chỉnh số dư.
 * Phụ thuộc: Wallets, WalletTopupRequests, WalletTransactions.
 * Yêu cầu: đặt Connection trong ServletContext key "DB_CONN".
 */
@WebServlet(name = "WalletAdminServlet", urlPatterns = {"/admin/wallet"})
public class WalletAdminServlet extends HttpServlet {

    // ===== DB wiring =====
    private Connection getConnection(HttpServletRequest req) {
        return (Connection) req.getServletContext().getAttribute("DB_CONN");
    }

    // ===== GET =====
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("topupsPending", loadTopupsByStatus(getConnection(req), "PENDING", 100));
            req.setAttribute("topupsRecent", loadRecentTopups(getConnection(req), 100));

            String userIdStr = req.getParameter("userId");
            if (userIdStr != null && userIdStr.matches("\\d+")) {
                int uid = Integer.parseInt(userIdStr);
                WalletDAO wdao = new WalletDAO(getConnection(req));
                req.setAttribute("queryUserId", uid);
                req.setAttribute("queryBalance", wdao.getBalance(uid));
            }

            req.getRequestDispatcher("/WEB-INF/views/admin/wallet.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    // ===== POST =====
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String act = req.getParameter("action");
        if ("adjust".equals(act)) {
            try { handleAdjust(req, resp); }
            catch (Exception e) { throw new ServletException(e); }
            return;
        }
        resp.sendRedirect(req.getContextPath()+"/admin/wallet");
    }

    private BigDecimal parseAmount(String s){
        if (s == null) throw new IllegalArgumentException("null");
        s = s.trim().replace(',', '.');
        BigDecimal v = new BigDecimal(s);
        if (v.scale() > 2) v = v.setScale(2, BigDecimal.ROUND_HALF_UP);
        return v;
    }

    private void handleAdjust(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String uidStr = req.getParameter("userId");
        String amountStr = req.getParameter("amount");
        String note = req.getParameter("note");

        if (uidStr == null || !uidStr.matches("\\d+")) {
            req.getSession().setAttribute("adminWalletError", "userId không hợp lệ");
            resp.sendRedirect(req.getContextPath() + "/admin/wallet");
            return;
        }
        BigDecimal amount;
        try { amount = parseAmount(amountStr); }
        catch (IllegalArgumentException ex) {
            req.getSession().setAttribute("adminWalletError", "Số tiền không hợp lệ");
            resp.sendRedirect(req.getContextPath() + "/admin/wallet");
            return;
        }

        int userId = Integer.parseInt(uidStr);
        try (Connection con = DB.getConnection()) {
            WalletDAO wdao = new WalletDAO(con);
            // điều chỉnh số dư, transaction Type = ADJUST, RefId = NULL
            wdao.addBalance(userId, amount, (Long) null, note != null ? note : "Admin adjust", "ADJUST");
            req.getSession().setAttribute("adminWalletInfo", "Điều chỉnh thành công");
        } catch (Exception ex) {
            req.getSession().setAttribute("adminWalletError", "Lỗi điều chỉnh: " + ex.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/admin/wallet");
    }

    private void handleForceSuccess(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String ridStr = req.getParameter("rid");
        if (ridStr == null || !ridStr.matches("\\d+")) {
            req.getSession().setAttribute("adminWalletError", "rid không hợp lệ");
            resp.sendRedirect(req.getContextPath() + "/admin/wallet");
            return;
        }
        long rid = Long.parseLong(ridStr);
        Connection con = getConnection(req);

        WalletTopupDAO tdao = new WalletTopupDAO(con);
        Topup t = tdao.load(rid);
        if (t == null || !"PENDING".equals(t.status)) {
            req.getSession().setAttribute("adminWalletError", "Không thể duyệt yêu cầu");
            resp.sendRedirect(req.getContextPath() + "/admin/wallet");
            return;
        }
        if (Instant.now().isAfter(t.expiresAt)) {
            tdao.markExpired(rid);
            req.getSession().setAttribute("adminWalletError", "Yêu cầu đã hết hạn");
            resp.sendRedirect(req.getContextPath() + "/admin/wallet");
            return;
        }

        WalletDAO wdao = new WalletDAO(con);
        con.setAutoCommit(false);
        try {
            wdao.addBalance(t.userId, t.amount, rid, "Admin force success topup #" + rid);
            tdao.markSuccess(rid);
            con.commit();
            req.getSession().setAttribute("adminWalletInfo", "Đã duyệt topup #" + rid);
        } catch (Exception ex) {
            con.rollback();
            req.getSession().setAttribute("adminWalletError", "Lỗi duyệt: " + ex.getMessage());
        } finally {
            con.setAutoCommit(true);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/wallet");
    }

    private void handleExpire(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String ridStr = req.getParameter("rid");
        if (ridStr == null || !ridStr.matches("\\d+")) {
            req.getSession().setAttribute("adminWalletError", "rid không hợp lệ");
            resp.sendRedirect(req.getContextPath() + "/admin/wallet");
            return;
        }
        long rid = Long.parseLong(ridStr);
        WalletTopupDAO tdao = new WalletTopupDAO(getConnection(req));
        tdao.markExpired(rid);
        req.getSession().setAttribute("adminWalletInfo", "Đã chuyển PENDING -> EXPIRED");
        resp.sendRedirect(req.getContextPath() + "/admin/wallet");
    }

    // ===== Queries =====
    private List<TopupRow> loadTopupsByStatus(Connection con, String status, int limit) throws SQLException {
        String sql = "SELECT TOP " + limit + " Id,UserId,Amount,Status,CreatedAt,ExpiresAt,Attempts " +
                     "FROM WalletTopupRequests WHERE Status=? ORDER BY CreatedAt DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                List<TopupRow> list = new ArrayList<>();
                while (rs.next()) {
                    TopupRow r = new TopupRow();
                    r.id = rs.getLong("Id");
                    r.userId = rs.getInt("UserId");
                    r.amount = rs.getBigDecimal("Amount");
                    r.status = rs.getString("Status");
                    r.createdAt = rs.getTimestamp("CreatedAt");
                    r.expiresAt = rs.getTimestamp("ExpiresAt");
                    r.attempts = rs.getInt("Attempts");
                    list.add(r);
                }
                return list;
            }
        }
    }

    private List<TopupRow> loadRecentTopups(Connection con, int limit) throws SQLException {
        String sql = "SELECT TOP " + limit + " Id,UserId,Amount,Status,CreatedAt,VerifiedAt " +
                     "FROM WalletTopupRequests WHERE Status <> 'PENDING' ORDER BY CreatedAt DESC";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<TopupRow> list = new ArrayList<>();
            while (rs.next()) {
                TopupRow r = new TopupRow();
                r.id = rs.getLong("Id");
                r.userId = rs.getInt("UserId");
                r.amount = rs.getBigDecimal("Amount");
                r.status = rs.getString("Status");
                r.createdAt = rs.getTimestamp("CreatedAt");
                r.verifiedAt = rs.getTimestamp("VerifiedAt");
                list.add(r);
            }
            return list;
        }
    }

    // ===== Utils =====


    // ===== DTO cho JSP =====
    public static class TopupRow {
        public long id;
        public int userId;
        public BigDecimal amount;
        public String status;
        public java.util.Date createdAt;
        public java.util.Date verifiedAt;
        public java.util.Date expiresAt;
        public int attempts;

        public long getId(){return id;}
        public int getUserId(){return userId;}
        public BigDecimal getAmount(){return amount;}
        public String getStatus(){return status;}
        public java.util.Date getCreatedAt(){return createdAt;}
        public java.util.Date getVerifiedAt(){return verifiedAt;}
        public java.util.Date getExpiresAt(){return expiresAt;}
        public int getAttempts(){return attempts;}
    }

    // ===== Alias giữ tương thích =====
    public BigDecimal getBalanceCompat(Connection con, int userId) throws SQLException {
        return new WalletDAO(con).getBalance(userId);
    }
    public void adjustBalanceCompat(Connection con, int userId, BigDecimal amount, String note) throws SQLException {
        new WalletDAO(con).addBalance(userId, amount, null, note);
    }
}
