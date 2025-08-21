package com.smartshop.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CartController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo(); // e.g. "/view"
        if (path == null || "/".equals(path)) {
            resp.sendRedirect(req.getContextPath() + "/cart/view");
            return;
        }
        switch (path) {
            case "/view" -> {
                req.setAttribute("cart", getCart(req));
                req.getRequestDispatcher("/WEB-INF/views/cart/view.jsp").forward(req, resp);
            }
            default -> resp.sendError(404);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String path = req.getPathInfo();
        if (path == null) { resp.sendError(404); return; }

        Map<Integer,Integer> cart = getCart(req);

        switch (path) {
            case "/add" -> {
                Integer pid = parseInt(req.getParameter("pid"));
                Integer qty = parseInt(req.getParameter("qty"));
                if (pid != null) {
                    int q = Math.max(1, qty == null ? 1 : qty);
                    cart.put(pid, cart.getOrDefault(pid, 0) + q);
                }
                resp.sendRedirect(req.getContextPath() + "/cart/view");
            }
            case "/update" -> {
                Integer pid = parseInt(req.getParameter("pid"));
                Integer qty = parseInt(req.getParameter("qty"));
                if (pid != null && qty != null) {
                    if (qty <= 0) cart.remove(pid);
                    else cart.put(pid, qty);
                }
                resp.sendRedirect(req.getContextPath() + "/cart/view");
            }
            case "/remove" -> {
                Integer pid = parseInt(req.getParameter("pid"));
                if (pid != null) cart.remove(pid);
                resp.sendRedirect(req.getContextPath() + "/cart/view");
            }
            case "/clear" -> {
                cart.clear();
                resp.sendRedirect(req.getContextPath() + "/cart/view");
            }
            default -> resp.sendError(404);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer,Integer> getCart(HttpServletRequest req) {
        HttpSession s = req.getSession(true);
        Object o = s.getAttribute("cart");
        if (o instanceof Map) return (Map<Integer,Integer>) o;
        Map<Integer,Integer> m = new LinkedHashMap<>();
        s.setAttribute("cart", m);
        return m;
    }

    private Integer parseInt(String s) {
        try { return s == null ? null : Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }
}
