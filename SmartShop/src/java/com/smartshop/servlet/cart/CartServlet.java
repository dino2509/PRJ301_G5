package com.smartshop.servlet.cart;

import com.smartshop.dao.ProductDAO;
import com.smartshop.dao.UserDAO;
import com.smartshop.model.CartItem;
import com.smartshop.model.Product;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@WebServlet(urlPatterns = {"/cart", "/cart/*"})
public class CartServlet extends HttpServlet {
    private static final String VIEW_CART = "/WEB-INF/views/shop/cart.jsp";
    private final ProductDAO productDao = new ProductDAO();

    @SuppressWarnings("unchecked")
    private Map<Integer, Object> getRawCart(HttpSession session) {
        Object o = session.getAttribute("cart");
        if (o == null) {
            Map<Integer, Object> cart = new LinkedHashMap<>();
            session.setAttribute("cart", cart);
            return cart;
        }
        return (Map<Integer, Object>) o;
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

        if (info == null || "/".equals(info)) {
            if (isUpdate(req))      info = "/update";
            else if (isRemove(req)) info = "/remove";
            else if (isAdd(req))    info = "/add";
            else                    info = "/";
        }

        HttpSession session = req.getSession();
        Map<Integer, Object> cart = getRawCart(session);

        if ("/".equals(info)) {
            req.setAttribute("cartItems", buildViewItems(cart));
            pushAcc(req);
            req.getRequestDispatcher(VIEW_CART).forward(req, resp);
            return;
        }

        int pid = readProductId(req);
        int qty = readQty(req);

        switch (info) {
            case "/add": {
                if (pid < 0) { resp.sendRedirect(req.getContextPath() + "/cart"); return; }
                Product p = productDao.findById(pid);
                if (p == null) { resp.sendRedirect(req.getContextPath() + "/cart"); return; }

                Object cur = cart.get(pid);
                if (cur instanceof CartItem) {
                    CartItem ci = (CartItem) cur;
                    int newQty = (ci.getQuantity() > 0 ? ci.getQuantity() : ci.getQty()) + (qty <= 0 ? 1 : qty);
                    ci.setQuantity(newQty);
                    ci.setQty(newQty);
                    ci.setName(p.getName());
                    ci.setPrice(p.getPrice());
                    ci.setImageUrl(p.getImageUrl());
                    ci.setProductId(p.getId());
                } else if (cur instanceof CartLine) {
                    CartLine cl = (CartLine) cur;
                    cl.qty += (qty <= 0 ? 1 : qty);
                } else {
                    CartItem item = new CartItem();
                    item.setProductId(p.getId());
                    item.setName(p.getName());
                    item.setPrice(p.getPrice());
                    item.setImageUrl(p.getImageUrl());
                    item.setQuantity(qty <= 0 ? 1 : qty);
                    item.setQty(item.getQuantity());
                    cart.put(pid, item);
                }
                break;
            }
            case "/update": {
                if (pid >= 0) {
                    Object cur = cart.get(pid);
                    if (cur instanceof CartItem) {
                        CartItem ci = (CartItem) cur;
                        if (qty <= 0) cart.remove(pid);
                        else { ci.setQuantity(qty); ci.setQty(qty); }
                    } else if (cur instanceof CartLine) {
                        if (qty <= 0) cart.remove(pid);
                        else ((CartLine) cur).qty = qty;
                    }
                }
                break;
            }
            case "/remove": {
                if (pid >= 0) cart.remove(pid);
                break;
            }
            default: {
                req.setAttribute("cartItems", buildViewItems(cart));
                pushAcc(req);
                req.getRequestDispatcher(VIEW_CART).forward(req, resp);
                return;
            }
        }
        resp.sendRedirect(req.getContextPath() + "/cart");
    }

    private List<ViewItem> buildViewItems(Map<Integer, Object> cart){
        List<ViewItem> list = new ArrayList<>();
        if (cart == null) return list;
        for (Map.Entry<Integer,Object> e : cart.entrySet()) {
            int pid = e.getKey();
            int qty = 0;
            Product p = null;

            if (e.getValue() instanceof CartItem) {
                CartItem it = (CartItem) e.getValue();
                qty = it.getQuantity() > 0 ? it.getQuantity() : it.getQty();
                BigDecimal price = it.getPrice()!=null ? it.getPrice() : it.getUnitPrice();
                String name = it.getName()!=null ? it.getName() : it.getProductName();
                String img = it.getImageUrl();
                p = productDao.findById(pid);
                if (p == null) { p = new Product(); p.setId(pid); p.setName(name); p.setPrice(price); p.setImageUrl(img); }
            } else if (e.getValue() instanceof CartLine) {
                CartLine cl = (CartLine) e.getValue();
                qty = Math.max(1, cl.qty);
                p = cl.product != null ? cl.product : productDao.findById(pid);
                if (p == null) { p = new Product(); p.setId(pid); }
            } else continue;

            if (p == null) p = productDao.findById(pid);
            if (p == null) continue;

            list.add(new ViewItem(p, qty));
        }
        return list;
    }

    private void pushAcc(HttpServletRequest req){
        HttpSession s = req.getSession(false);
        Integer userId = null;
        if (s != null) {
            Object v = s.getAttribute("uid");
            if (v instanceof Integer) userId = (Integer) v;
            else if (v instanceof String) try { userId = Integer.valueOf((String) v);} catch (Exception ignore){}
        }
        Object accObj = (s != null)? s.getAttribute("authUser") : null;

        String fullName = null, phone = null, email = null, address = null;
        try {
            if (accObj != null) {
                fullName = (String) accObj.getClass().getMethod("getFullName").invoke(accObj);
                phone    = (String) accObj.getClass().getMethod("getPhone").invoke(accObj);
                email    = (String) accObj.getClass().getMethod("getEmail").invoke(accObj);
                address  = (String) accObj.getClass().getMethod("getAddress").invoke(accObj);
            } else if (userId != null) {
                var u = new UserDAO().findById(userId);
                if (u != null) { fullName=u.getFullName(); phone=u.getPhone(); email=u.getEmail(); address=u.getAddress(); }
            }
        } catch (Exception ignore) { }
        req.setAttribute("accFullName", fullName);
        req.setAttribute("accPhone", phone);
        req.setAttribute("accEmail", email);
        req.setAttribute("accAddress", address);
    }

    private boolean isAdd(HttpServletRequest req) {
        String act = lower(pickFirst(req, "action", "op", "act", "btn", "submit"));
        if ("add".equals(act) || hasParam(req, "add") || hasParam(req, "addToCart")) return true;
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
        String v = pickFirst(req, "id", "productId", "pid", "product_id", "pId", "prodId");
        return parseInt(v, -1);
    }
    private int readQty(HttpServletRequest req) {
        String v = pickFirst(req, "qty", "quantity", "q");
        int q = parseInt(v, 1);
        return q <= 0 ? 1 : q;
    }
    private String pickFirst(HttpServletRequest req, String... names){
        for (String n : names) {
            String v = req.getParameter(n);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
    private boolean hasParam(HttpServletRequest req, String name) { return req.getParameter(name) != null; }
    private String lower(String s) { return s == null ? null : s.toLowerCase(); }
    private int parseInt(String s, int defVal) { try { return Integer.parseInt(s); } catch (Exception e) { return defVal; } }

    // Legacy compatibility
    static class CartLine { Product product; int qty; CartLine(Product p, int q){ this.product=p; this.qty=q; } }
    static class ViewItem { public final Product product; public final int qty; ViewItem(Product p, int q){ this.product=p; this.qty=q; } public Product getProduct(){ return product; } public int getQty(){ return qty; } }
}
