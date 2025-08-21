package com.smartshop.servlet.shop;

import com.smartshop.dao.CartDAO;
import com.smartshop.dao.OrderDAO;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;

@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();
    private final CartDAO cartDAO = new CartDAO();

    private int uid(HttpServletRequest req) {
        return (Integer) req.getSession().getAttribute("uid");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("items", new com.smartshop.dao.CartDAO().listItems(uid(req)));
        req.setAttribute("total", cartDAO.total(uid(req)));
        req.getRequestDispatcher("/WEB-INF/views/shop/checkout.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getParameter("name");
        String phone = req.getParameter("phone");
        String addr = req.getParameter("address");
        String pay = req.getParameter("payment");
        int orderId = orderDAO.createOrderFromCart(uid(req), name, phone, addr, pay);
        req.getSession().setAttribute("flash", "Order placed: #" + orderId);
        resp.sendRedirect(req.getContextPath() + "/orders/success.jsp?orderId=" + orderId);
    }
}
