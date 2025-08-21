package com.smartshop.servlet;

import com.smartshop.dao.ProductDAO;
import com.smartshop.model.Product;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@WebServlet(name = "SearchController", urlPatterns = {"/search"})
public class SearchController extends HttpServlet {

    private final ProductDAO dao = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String q      = trim(req.getParameter("q"));
        String brand  = trim(req.getParameter("brand"));
        Integer cat   = parseInt(req.getParameter("cat"));
        BigDecimal min= parseDec(req.getParameter("min"));
        BigDecimal max= parseDec(req.getParameter("max"));
        String sort   = trim(req.getParameter("sort")); // price_asc|price_desc|newest|sold|rating

        Integer pg = parseInt(req.getParameter("page"));
        Integer sz = parseInt(req.getParameter("size"));
        int page = (pg == null || pg < 1) ? 1 : pg;
        int size = (sz == null || sz < 1 || sz > 96) ? 24 : sz;

        int total = dao.countAll(q, cat, brand, null, min, max);
        int pages = (int) Math.ceil(total / (double) size);
        if (pages > 0 && page > pages) page = pages;

        int offset = (page - 1) * size;
        List<Product> items = dao.search(q, cat, brand, null, min, max, sort, offset, size);

        req.setAttribute("q", q);
        req.setAttribute("brand", brand);
        req.setAttribute("cat", cat);
        req.setAttribute("min", min);
        req.setAttribute("max", max);
        req.setAttribute("sort", sort);
        req.setAttribute("page", page);
        req.setAttribute("pages", pages);
        req.setAttribute("total", total);
        req.setAttribute("items", items);

        req.getRequestDispatcher("/WEB-INF/views/search.jsp").forward(req, resp);
    }

    private static String trim(String s){
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
    private static Integer parseInt(String s){
        try { return (s==null||s.isBlank())?null:Integer.valueOf(s.trim()); }
        catch(Exception e){ return null; }
    }
    private static BigDecimal parseDec(String s){
        try { return (s==null||s.isBlank())?null:new BigDecimal(s.trim()); }
        catch(Exception e){ return null; }
    }
}
