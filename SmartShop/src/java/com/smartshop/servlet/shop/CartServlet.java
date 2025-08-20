package com.smartshop.servlet.shop;

import com.smartshop.model.CartItem;
import com.smartshop.dao.CartDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(urlPatterns = {"/cart", "/cart/*"})
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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handle(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handle(req, resp);
    }

    private void handle(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String info = req.getPathInfo(); // null, "/", "/add", "/update", "/remove"
        String actionParam = pickFirst(req, "action", "op", "act");

        // Chuẩn hoá action khi pathInfo rỗng: tự đoán theo tham số
        if (info == null || "/".equals(info)) {
            if (isUpdate(req))      info = "/update";
            else if (isRemove(req)) info = "/remove";
            else if (isAdd(req))    info = "/add";
            else                    info = "/"; // xem giỏ
        }

        if ("/".equals(info)) {
            req.getRequestDispatcher(VIEW_CART).forward(req, resp);
            return;
        }

        HttpSession session = req.getSession();
        Map<Integer, CartItem> cart = getCart(session);

        int pid = readProductId(req);
        int qty = readQty(req);

        switch (info) {
            case "/add": {
                if (pid < 0) { // không có id → quay về giỏ
                    resp.sendRedirect(req.getContextPath() + "/cart");
                    return;
                }
                BigDecimal price = CartDAO.safeGetProductPrice(pid, new BigDecimal("1.00"));
                CartItem item = cart.getOrDefault(pid,
                        new CartItem(pid, "Product #" + pid, price, 0, null));
                item.setQuantity(item.getQuantity() + qty);
                cart.put(pid, item);

                Integer uid = (Integer) session.getAttribute("uid");
                if (uid != null) CartDAO.safeAddItemForUser(uid, pid, qty, price);
                break;
            }
            case "/update": {
                if (pid >= 0) {
                    CartItem item = cart.get(pid);
                    if (item != null) {
                        if (qty <= 0) { cart.remove(pid); }
                        else { item.setQuantity(qty); }
                        Integer uid = (Integer) session.getAttribute("uid");
                        if (uid != null) {
                            if (qty <= 0) CartDAO.safeRemoveItemForUser(uid, pid);
                            else          CartDAO.safeSetItemQtyForUser(uid, pid, qty, item.getPrice());
                        }
                    }
                }
                break;
            }
            case "/remove": {
                if (pid >= 0) {
                    CartItem item = cart.remove(pid);
                    Integer uid = (Integer) session.getAttribute("uid");
                    if (uid != null) CartDAO.safeRemoveItemForUser(uid, pid);
                }
                break;
            }
            default:
                break;
        }

        resp.sendRedirect(req.getContextPath() + "/cart");
    }

    // ---------- helpers ----------
    private boolean isAdd(HttpServletRequest req) {
        String act = lower(pickFirst(req, "action", "op", "act", "btn", "submit"));
        if ("add".equals(act) || hasParam(req, "add") || hasParam(req, "addToCart")) return true;
        // Nếu có id sản phẩm mà không phải update/remove → coi như add
        return readProductId(req) >= 0 && !isUpdate(req) && !isRemove(req);
    }

    private boolean isUpdate(HttpServletRequest req) {
        String act = lower(pickFirst(req, "action", "op", "act", "btn", "submit"));
        return "update".equals(act) || hasParam(req, "update");
    }

    private boolean isRemove(HttpServletRequest req) {
        String act = lower(pickFirst(req, "action", "op", "act", "btn", "submit"));
        return "remove".equals(act) || hasParam(req, "remove") || hasParam(req, "delete");
    }

    private int readProductId(HttpServletRequest req) {
        // chấp nhận nhiều tên tham số id
        String v = pickFirst(req, "id", "productId", "pid", "product_id", "pId", "prodId");
        return parseInt(v, -1);
    }

    private int readQty(HttpServletRequest req) {
        String v = pickFirst(req, "qty", "quantity", "q");
        int n = parseInt(v, 1);
        return n < 1 ? 1 : n;
    }

    private String pickFirst(HttpServletRequest req, String... names) {
        for (String n : names) {
            String v = req.getParameter(n);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private boolean hasParam(HttpServletRequest req, String name) {
        return req.getParameter(name) != null;
    }

    private String lower(String s) { return s == null ? null : s.toLowerCase(); }

    private int parseInt(String s, int defVal) {
        try { return Integer.parseInt(s); } catch (Exception e) { return defVal; }
    }
}
