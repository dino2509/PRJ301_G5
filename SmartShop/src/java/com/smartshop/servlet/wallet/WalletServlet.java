// src/main/java/com/smartshop/servlet/wallet/WalletServlet.java
package com.smartshop.servlet.wallet;

import com.smartshop.dao.UserDAO;
import com.smartshop.dao.WalletDAO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;

@WebServlet(urlPatterns = {"/wallet"})
public class WalletServlet extends HttpServlet {

    private Integer uid(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return null;
        Object v = s.getAttribute("uid");
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof String) try { return Integer.valueOf((String)v); } catch(Exception ignored){}
        return null;
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer userId = uid(req);
        if (userId == null) { resp.sendRedirect(req.getContextPath()+"/login?next=/wallet"); return; }

        WalletDAO w = new WalletDAO();
        req.setAttribute("balance", w.getBalance(userId));
        req.setAttribute("profile", new UserDAO().getProfile(userId));

        RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/views/wallet/index.jsp");
        rd.forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer userId = uid(req);
        if (userId == null) { resp.sendRedirect(req.getContextPath()+"/login?next=/wallet"); return; }

        if (!"topup".equals(req.getParameter("action"))) { doGet(req, resp); return; }

        String raw = req.getParameter("amount");
        try {
            if (raw == null) throw new IllegalArgumentException("null");
            // Bỏ mọi ký tự không phải số. VND là tiền nguyên nên không cần phần thập phân.
            String digits = raw.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) throw new IllegalArgumentException("empty");
            BigDecimal amount = new BigDecimal(digits);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                req.setAttribute("error", "Số tiền nạp phải > 0.");
            } else {
                new WalletDAO().topup(userId, amount, "User top-up");
                req.setAttribute("success", "Đã nạp " + amount + " vào ví.");
            }
        } catch (Exception e) {
            req.setAttribute("error", "Số tiền không hợp lệ.");
        }
        doGet(req, resp);
    }
}
