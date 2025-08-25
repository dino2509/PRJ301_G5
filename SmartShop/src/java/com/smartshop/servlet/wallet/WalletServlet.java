package com.smartshop.servlet.wallet;

import com.smartshop.dao.WalletDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;

@WebServlet(urlPatterns = {"/wallet"})
public class WalletServlet extends HttpServlet {
    private static final String VIEW = "/WEB-INF/views/wallet/index.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer userId = uid(req);
        try (WalletDAO wdao = new WalletDAO()) {
            req.setAttribute("balance", wdao.getBalance(userId));
        } catch (Exception e) {
            req.setAttribute("balance", new BigDecimal("0"));
            req.setAttribute("walletError", "Không lấy được số dư: " + e.getMessage());
        }
        // flash
        HttpSession s = req.getSession(false);
        if (s != null) {
            Object ok = s.getAttribute("walletSuccess");
            Object er = s.getAttribute("walletError");
            if (ok != null) { req.setAttribute("walletSuccess", ok); s.removeAttribute("walletSuccess"); }
            if (er != null) { req.setAttribute("walletError", er); s.removeAttribute("walletError"); }
        }
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = nvl(req.getParameter("action"));
        if ("topup".equalsIgnoreCase(action)) { handleTopup(req, resp); return; }
        resp.sendRedirect(req.getContextPath() + "/wallet");
    }

    private void handleTopup(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession();
        Integer userId = uid(req);
        String amountStr = nvl(req.getParameter("amount"));
        try {
            BigDecimal amount = new BigDecimal(amountStr.trim());
            if (amount.scale() > 2) amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Số tiền phải > 0");
            try (WalletDAO wdao = new WalletDAO()) {
                wdao.addBalance(userId, amount, null, "User topup", "TOPUP");
            }
            s.setAttribute("walletSuccess", "Nạp " + amount + " thành công.");
        } catch (Exception e) {
            s.setAttribute("walletError", "Nạp thất bại: " + e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/wallet");
    }

    private Integer uid(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        Object o = s == null ? null : s.getAttribute("uid");
        if (o instanceof Integer) return (Integer) o;
        throw new RuntimeException("Please login");
    }
    private static String nvl(String s){ return s==null?"":s; }
}
