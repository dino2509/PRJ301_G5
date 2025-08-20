package com.smartshop.servlet.shop;

import com.smartshop.dao.ReviewDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet(urlPatterns={"/product/review"})
public class ReviewServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int productId = parseInt(req.getParameter("productId"), -1);
        resp.sendRedirect(req.getContextPath() + "/product?id=" + productId);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer uid = (Integer) req.getSession().getAttribute("uid");
        if (uid == null) {
            resp.sendRedirect(req.getContextPath() + "/login?next=" + req.getRequestURI());
            return;
        }
        int productId = parseInt(req.getParameter("productId"), -1);
        int rating = parseInt(req.getParameter("rating"), 5);
        String comment = req.getParameter("comment");

        try {
            new ReviewDAO().upsert(productId, uid, rating, comment);
            resp.sendRedirect(req.getContextPath() + "/product?id=" + productId + "&review=ok");
        } catch (RuntimeException ex) {
            resp.sendRedirect(req.getContextPath() + "/product?id=" + productId + "&review=fail");
        }
    }

    private int parseInt(String s, int defVal) { try { return Integer.parseInt(s); } catch (Exception e) { return defVal; } }
}
