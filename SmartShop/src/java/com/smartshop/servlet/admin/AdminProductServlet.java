package com.smartshop.servlet.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.RequestDispatcher;
import java.io.IOException;

@WebServlet(urlPatterns = {"/admin/products", "/admin/products/*"})
public class AdminProductServlet extends HttpServlet {
    private static final String VIEW_LIST = "/WEB-INF/views/admin/products.jsp";

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RequestDispatcher rd = req.getRequestDispatcher(VIEW_LIST);
        rd.forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/admin/products");
    }
}
