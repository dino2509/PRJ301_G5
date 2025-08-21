package com.smartshop.servlet.shop;

import com.smartshop.dao.CartDAO;
import com.smartshop.dao.OrderDAO;
import com.smartshop.dao.WalletDAO;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class CheckoutServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();
    private final CartDAO cartDAO = new CartDAO();
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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // keep current checkout view
        req.getRequestDispatcher("/WEB-INF/views/shop/checkout.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        String phone = req.getParameter("phone");
        String addr = req.getParameter("address");
        String pay = req.getParameter("payment"); // COD | WALLET | FAKE
        int userId = uid(req);

        int orderId = orderDAO.createOrderFromCart(userId, name, phone, addr, pay);
        if ("WALLET".equalsIgnoreCase(pay)) {
            try {
                walletDAO.payOrderWithWallet(userId, orderId);
                req.getSession().setAttribute("flash", "Paid by wallet. Order #" + orderId);
                resp.sendRedirect(req.getContextPath() + "/orders/success.jsp?orderId=" + orderId);
                return;
            } catch (RuntimeException ex) {
                req.getSession().setAttribute("flash", "Wallet payment failed: " + ex.getMessage());
                resp.sendRedirect(req.getContextPath() + "/wallet");
                return;
            }
        } else if ("FAKE".equalsIgnoreCase(pay)) {
            resp.sendRedirect(req.getContextPath() + "/fakepay?orderId=" + orderId);
            return;
        } else {
            // COD or others
            req.getSession().setAttribute("flash", "Order placed: #" + orderId + " (COD)");
            resp.sendRedirect(req.getContextPath() + "/orders/success.jsp?orderId=" + orderId);
        }
    }
}
