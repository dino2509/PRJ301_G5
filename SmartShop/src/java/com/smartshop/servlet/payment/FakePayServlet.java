package com.smartshop.servlet.payment;

import com.smartshop.util.DB;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;

public class FakePayServlet extends HttpServlet {

    private static final String SECRET = "demo-secret-123";

    private int uid(HttpServletRequest req) {
        Object u = req.getSession().getAttribute("user");
        if (u == null) throw new RuntimeException("Please login");
        try {
            java.lang.reflect.Method m = u.getClass().getMethod("getId");
            return (int) m.invoke(u);
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve user id", e);
        }
    }

    private BigDecimal orderAmount(int orderId, int userId) {
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

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int orderId = Integer.parseInt(req.getParameter("orderId"));
        int userId = uid(req);
        java.math.BigDecimal amount = orderAmount(orderId, userId);
        req.setAttribute("orderId", orderId);
        req.setAttribute("amount", amount);
        req.getRequestDispatcher("/WEB-INF/views/payment/fakepay.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action"); // success | fail
        int orderId = Integer.parseInt(req.getParameter("orderId"));
        int userId = uid(req);
        java.math.BigDecimal amount = orderAmount(orderId, userId);
        if ("success".equalsIgnoreCase(action)) {
            String txnId = "FAKE-" + orderId + "-" + System.currentTimeMillis();
            String sig = Integer.toHexString((txnId + amount + SECRET).hashCode());
            resp.sendRedirect(req.getContextPath() + "/payment/ipn?provider=FAKE&orderId=" + orderId +
                    "&amount=" + amount + "&txnId=" + txnId + "&sig=" + sig);
        } else {
            req.getSession().setAttribute("flash", "Fake payment canceled");
            resp.sendRedirect(req.getContextPath() + "/");
        }
    }
}
