package com.smartshop.servlet.wallet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet(urlPatterns = {"/transaction_history", "/transcation_history"})
public class TransactionHistoryServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer uid = (Integer) req.getSession().getAttribute("uid");
        if (uid == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }

        try (Connection con = com.smartshop.util.DB.getConnection()) {
            req.setAttribute("walletTx", queryWalletTx(con, uid));
            req.setAttribute("orderTx",  queryOrderTx(con, uid));
        } catch (Exception e) {
            req.setAttribute("txError", e.getMessage());
        }
        req.getRequestDispatcher("/WEB-INF/views/shop/transaction_history.jsp").forward(req, resp);
    }

    private List<Map<String,Object>> queryOrderTx(Connection con, int uid) {
        List<Map<String,Object>> rows = new ArrayList<>();
        String sql = "SELECT TOP 200 transaction_code, type, amount, method, status, created_at " +
                     "FROM dbo.Transactions WHERE user_id=? ORDER BY created_at DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("code", rs.getString(1));
                    m.put("type", rs.getString(2));
                    m.put("amount", rs.getBigDecimal(3));
                    m.put("method", rs.getString(4));
                    m.put("status", rs.getString(5));
                    m.put("created_at", rs.getTimestamp(6));
                    rows.add(m);
                }
            }
        } catch (SQLException ignore) {}
        return rows;
    }

    private List<Map<String,Object>> queryWalletTx(Connection con, int uid) {
        // cột có thể khác nhau giữa máy — cố gắng đọc an toàn
        String sql = "SELECT TOP 200 " +
                "COALESCE(try_cast(transaction_id as nvarchar(50)), try_cast(id as nvarchar(50))) AS id, " +
                "amount, " +
                "COALESCE([type],'') AS type, " +
                "COALESCE([status],'') AS status, " +
                "COALESCE([note],'') AS note, " +
                "COALESCE([ref_id], NULL) AS ref_id, " +
                "COALESCE([created_at], SYSUTCDATETIME()) AS created_at " +
                "FROM dbo.WalletTransactions WHERE user_id=? ORDER BY created_at DESC";
        List<Map<String,Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getString("id"));
                    m.put("amount", rs.getBigDecimal("amount"));
                    m.put("type", rs.getString("type"));
                    m.put("status", rs.getString("status"));
                    m.put("note", rs.getString("note"));
                    m.put("ref_id", rs.getObject("ref_id"));
                    m.put("created_at", rs.getTimestamp("created_at"));
                    rows.add(m);
                }
            }
        } catch (SQLException ignore) {}
        return rows;
    }
}
