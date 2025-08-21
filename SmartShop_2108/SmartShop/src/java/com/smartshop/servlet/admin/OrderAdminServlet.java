package com.smartshop.servlet.admin;

import com.smartshop.dao.OrderDAO;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;

@WebServlet("/admin/orders")
public class OrderAdminServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("orders", orderDAO.listAll());
        req.getRequestDispatcher("/WEB-INF/views/admin/orders.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String act = req.getParameter("action");
        if ("status".equals(act)) {
            orderDAO.updateStatus(Integer.parseInt(req.getParameter("id")), req.getParameter("status"));
        } else if ("delete".equals(act)) {
            orderDAO.deleteIfUnpaid(Integer.parseInt(req.getParameter("id")));
        }
        resp.sendRedirect(req.getContextPath() + "/admin/orders");
    }
}
