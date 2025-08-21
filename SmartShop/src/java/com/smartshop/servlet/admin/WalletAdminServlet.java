package com.smartshop.servlet.admin;

import com.smartshop.dao.WalletDAO;
import com.smartshop.util.DB;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WalletAdminServlet extends HttpServlet {
    private final WalletDAO walletDAO = new WalletDAO();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // List pending topups
        String sql = "SELECT t.id, u.username, t.amount, t.created_at " +
                "FROM dbo.WalletTransactions t JOIN dbo.Users u ON t.user_id=u.id " +
                "WHERE t.type='TOPUP' AND t.status='PENDING' ORDER BY t.id";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getBigDecimal(3), rs.getTimestamp(4)});
        } catch (SQLException e) { throw new RuntimeException(e); }
        req.setAttribute("rows", rows);
        req.getRequestDispatcher("/WEB-INF/views/admin/wallet-topups.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action"); // approve|reject
        int txId = Integer.parseInt(req.getParameter("txId"));
        if ("approve".equals(action)) {
            walletDAO.approveTopup(txId);
            req.getSession().setAttribute("flash", "Approved topup #" + txId);
        } else if ("reject".equals(action)) {
            try (java.sql.Connection c = com.smartshop.util.DB.getConnection();
                 java.sql.PreparedStatement ps = c.prepareStatement("UPDATE dbo.WalletTransactions SET status='REJECTED' WHERE id=?")) {
                ps.setInt(1, txId); ps.executeUpdate();
            } catch (Exception e) { throw new RuntimeException(e); }
            req.getSession().setAttribute("flash", "Rejected topup #" + txId);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/wallet-topups");
    }
}
