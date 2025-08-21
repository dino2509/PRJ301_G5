package com.smartshop.servlet.wallet;

import com.smartshop.dao.WalletDAO;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;

public class WalletServlet extends HttpServlet {
    private final WalletDAO walletDAO = new WalletDAO();

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

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int userId = uid(req);
        req.setAttribute("balance", walletDAO.getBalance(userId));
        req.setAttribute("txs", walletDAO.listTx(userId, 50));
        req.getRequestDispatcher("/WEB-INF/views/wallet/index.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int userId = uid(req);
        String action = req.getParameter("action");
        if ("topup_request".equals(action)) {
            BigDecimal amount = new BigDecimal(req.getParameter("amount"));
            walletDAO.createTopupRequest(userId, amount);
            req.getSession().setAttribute("flash", "Topup request submitted");
        }
        resp.sendRedirect(req.getContextPath() + "/wallet");
    }
}
