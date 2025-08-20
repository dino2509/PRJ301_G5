package com.smartshop.servlet.shop;

import com.smartshop.dao.ProductDAO;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;

@WebServlet("/products")
public class ProductListServlet extends HttpServlet {

    private final ProductDAO dao = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String q = req.getParameter("q");
        Integer cat = req.getParameter("categoryId") == null || req.getParameter("categoryId").isBlank() ? null : Integer.valueOf(req.getParameter("categoryId"));
        String brand = req.getParameter("brand");
        String color = req.getParameter("color");
        java.math.BigDecimal min = paramDecimal(req.getParameter("min"));
        java.math.BigDecimal max = paramDecimal(req.getParameter("max"));
        String sort = req.getParameter("sort");
        int page = parseInt(req.getParameter("page"), 1);
        int size = 10;
        req.setAttribute("products", dao.search(q, cat, brand, color, min, max, sort, page, size));
        req.getRequestDispatcher("/products.jsp").forward(req, resp);
    }

    private static int parseInt(String s, int d) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return d;
        }
    }

    private static java.math.BigDecimal paramDecimal(String s) {
        try {
            return s == null || s.isBlank() ? null : new java.math.BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }
}
