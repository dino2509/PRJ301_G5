package com.smartshop.servlet;

import com.smartshop.dao.ProductDAO;
import com.smartshop.model.Product;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@WebServlet("/home")
public class HomeController extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String q = req.getParameter("q");
        String brand = req.getParameter("brand");
        String color = req.getParameter("color");
        String sort = req.getParameter("sort");
        Integer categoryId = req.getParameter("categoryId") == null ? null : Integer.parseInt(req.getParameter("categoryId"));
        java.math.BigDecimal min = null, max = null;
        try {
            if (req.getParameter("min") != null) {
                min = new java.math.BigDecimal(req.getParameter("min"));
            }
        } catch (Exception ignored) {
        }
        try {
            if (req.getParameter("max") != null) {
                max = new java.math.BigDecimal(req.getParameter("max"));
            }
        } catch (Exception ignored) {
        }
        int page = 1, size = 10;
        try {
            page = Integer.parseInt(req.getParameter("page"));
        } catch (Exception ignored) {
        }
        int total = productDAO.countAll(q, categoryId, brand, color, min, max);
        int offset = (page - 1) * size;
        java.util.List<Product> list = productDAO.search(q, categoryId, brand, color, min, max, sort, offset, size);
        req.setAttribute("products", list);
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("size", size);
        req.setAttribute("featured", productDAO.top("featured", 8));
        req.setAttribute("newest", productDAO.top("new", 8));
        req.setAttribute("bestseller", productDAO.top("bestseller", 8));
        req.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(req, resp);

        req.setAttribute("dbStatus", com.smartshop.dao.DBContext.ping());

    }
}
