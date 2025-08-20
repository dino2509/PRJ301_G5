package com.smartshop.servlet.shop;

import com.smartshop.model.CartItem;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.RequestDispatcher;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@WebServlet(urlPatterns = {"/cart","/cart/*","/cart/add","/cart/update","/cart/remove"})
public class CartServlet extends HttpServlet {
    private static final String VIEW_CART = "/WEB-INF/views/shop/cart.jsp";

    @SuppressWarnings("unchecked")
    private Map<Integer, CartItem> getCart(HttpSession session) {
        Object o = session.getAttribute("cart");
        if (o == null) {
            Map<Integer, CartItem> cart = new LinkedHashMap<>();
            session.setAttribute("cart", cart);
            return cart;
        }
        return (Map<Integer, CartItem>) o;
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String p = req.getServletPath();
        if ("/cart".equals(p)) {
            req.getRequestDispatcher(VIEW_CART).forward(req, resp);
            return;
        }
        doAction(req, resp); // hỗ trợ GET cho add/update/remove
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doAction(req, resp);
    }

    private void doAction(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String p = req.getServletPath();
        Map<Integer, CartItem> cart = getCart(req.getSession());

        int id = parseInt(req.getParameter("id"), -1);
        int qty = parseInt(req.getParameter("qty"), 1);
        if (qty < 1) qty = 1;

        switch (p) {
            case "/cart/add": {
                CartItem item = cart.getOrDefault(id,
                        new CartItem(id, "Product #" + id, new BigDecimal("1.00"), 0, null));
                item.setQuantity(item.getQuantity() + qty);
                cart.put(id, item);
                break;
            }
            case "/cart/update": {
                CartItem item = cart.get(id);
                if (item != null) item.setQuantity(qty);
                break;
            }
            case "/cart/remove": {
                cart.remove(id);
                break;
            }
            default: break;
        }
        resp.sendRedirect(req.getContextPath() + "/cart");
    }

    private int parseInt(String s, int defVal) { try { return Integer.parseInt(s); } catch (Exception e) { return defVal; } }
}
