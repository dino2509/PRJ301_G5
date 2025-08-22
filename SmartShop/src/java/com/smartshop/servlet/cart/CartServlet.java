package com.smartshop.servlet.cart;

import com.smartshop.dao.OrderDAO;
import com.smartshop.dao.UserDAO;
import com.smartshop.dao.WalletDAO;
import com.smartshop.model.CartItem;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@WebServlet(name = "CartServlet", urlPatterns = {"/cart", "/cart/*"})
public class CartServlet extends HttpServlet {

    // ví dụ package của bạn: com.smartshop.servlet.shop
    private void pushAcc(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        Integer userId = null;
        if (s != null) {
            Object v = s.getAttribute("uid");
            if (v instanceof Integer) {
                userId = (Integer) v;
            } else if (v instanceof String) try {
                userId = Integer.valueOf((String) v);
            } catch (Exception ignore) {
            }
        }

        // Ưu tiên lấy object user sẵn có trong session nếu bạn đã set
        Object accObj
                = (s != null && s.getAttribute("authUser") != null) ? s.getAttribute("authUser")
                : (s != null && s.getAttribute("user") != null) ? s.getAttribute("user")
                : (s != null && s.getAttribute("account") != null) ? s.getAttribute("account") : null;

        String fullName = null, phone = null, email = null, address = null;

        try {
            if (accObj != null) {
                // getter phải tồn tại: getFullName/getPhone/getEmail/getAddress
                var u = accObj;
                fullName = (String) u.getClass().getMethod("getFullName").invoke(u);
                phone = (String) u.getClass().getMethod("getPhone").invoke(u);
                email = (String) u.getClass().getMethod("getEmail").invoke(u);
                address = (String) u.getClass().getMethod("getAddress").invoke(u);
            } else if (userId != null) {
                // fallback DB
                var u = new com.smartshop.dao.UserDAO().findById(userId);
                if (u != null) {
                    fullName = u.getFullName();
                    phone = u.getPhone();
                    email = u.getEmail();
                    address = u.getAddress();
                }
            }
        } catch (Exception ignore) {
        }

        if (fullName == null) {
            fullName = "";
        }
        if (phone == null) {
            phone = "";
        }
        if (email == null) {
            email = "";
        }
        if (address == null) {
            address = "";
        }

        req.setAttribute("accFullName", fullName);
        req.setAttribute("accPhone", phone);
        req.setAttribute("accEmail", email);
        req.setAttribute("accAddress", address);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession s = req.getSession(true);
        Map<Integer, CartItem> cart = getCart(s);
        BigDecimal total = cart.values().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // set account info vào request để cart.jsp auto đổ
        Integer userId = uid(req);
        if (userId != null) {
            var u = new UserDAO().findById(userId);
            if (u != null) {
                req.setAttribute("accFullName", nvl(u.getFullName()));
                req.setAttribute("accPhone", nvl(u.getPhone()));
                req.setAttribute("accEmail", nvl(u.getEmail()));
                req.setAttribute("accAddress", nvl(u.getAddress()));
            }
        }

        req.setAttribute("total", total);
        pushAcc(req);
        req.getRequestDispatcher("/WEB-INF/views/shop/cart.jsp").forward(req, resp); // phải là forward, không redirect

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getPathInfo();
        String action = req.getParameter("action");

        if (path != null) {
            path = path.trim();
        }
        if ("/update".equalsIgnoreCase(path)) {
            update(req, resp);
            return;
        }
        if ("/remove".equalsIgnoreCase(path)) {
            remove(req, resp);
            return;
        }
        if ("/clear".equalsIgnoreCase(path)) {
            clear(req, resp);
            return;
        }
        if ("/add".equalsIgnoreCase(path)) {
            add(req, resp);
            return;
        }

        if ("placeOrder".equalsIgnoreCase(action) || path == null || "/".equals(path)) {
            placeOrder(req, resp);
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void add(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(true);
        Map<Integer, CartItem> cart = getCart(s);

        int id = parseInt(req.getParameter("id"), -1);
        int qty = Math.max(1, parseInt(req.getParameter("qty"), 1));
        if (id <= 0) {
            resp.sendRedirect(req.getContextPath() + "/cart");
            return;
        }

        CartItem it = cart.get(id);
        if (it == null) {
            // Tối thiểu tạo item với id + qty; name/price/image nên được điền ở nơi khác tuỳ dự án
            it = new CartItem();
            it.setProductId(id);
            it.setName("Product #" + id);
            it.setPrice(new java.math.BigDecimal("0"));
            it.setQuantity(qty);
            cart.put(id, it);
        } else {
            it.setQuantity(it.getQuantity() + qty);
        }
        s.setAttribute("cart", cart);
        resp.sendRedirect(req.getContextPath() + "/cart");
    }

    private void update(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(true);
        Map<Integer, CartItem> cart = getCart(s);

        int id = parseInt(req.getParameter("id"), -1);
        int qty = Math.max(1, parseInt(req.getParameter("qty"), 1));
        CartItem it = cart.get(id);
        if (it != null) {
            it.setQuantity(qty);
        }

        s.setAttribute("cart", cart);
        resp.sendRedirect(req.getContextPath() + "/cart");
    }

    private void remove(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(true);
        Map<Integer, CartItem> cart = getCart(s);
        int id = parseInt(req.getParameter("id"), -1);
        cart.remove(id);
        s.setAttribute("cart", cart);
        resp.sendRedirect(req.getContextPath() + "/cart");
    }

    private void clear(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(true);
        s.setAttribute("cart", new LinkedHashMap<Integer, CartItem>());
        resp.sendRedirect(req.getContextPath() + "/cart");
    }

    private void placeOrder(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        HttpSession s = req.getSession(true);
        Map<Integer, CartItem> cart = getCart(s);
        if (cart.isEmpty()) {
            req.setAttribute("error", "Giỏ hàng trống");
            doGet(req, resp);
            return;
        }

        Integer userId = uid(req);
        if (userId == null) {
            throw new RuntimeException("Please login");
        }

        String pm = nvl(req.getParameter("pm")); // COD | WALLET | GATEWAY
        if (pm.isEmpty()) {
            pm = "COD";
        }

        // Nếu dùng account thì ép từ DB, bỏ qua input client
        boolean useAcc = "1".equals(req.getParameter("useAccount"));
        String fullName, phone, email, address;
        if (useAcc) {
            var u = new UserDAO().findById(userId);
            if (u == null) {
                throw new RuntimeException("Please login");
            }
            fullName = nvl(u.getFullName());
            phone = nvl(u.getPhone());
            email = nvl(u.getEmail());
            address = nvl(u.getAddress());
        } else {
            fullName = nvl(req.getParameter("fullName"));
            phone = nvl(req.getParameter("phone"));
            email = nvl(req.getParameter("email"));
            address = nvl(req.getParameter("address"));
        }

        // Tính tiền
        java.math.BigDecimal total = cart.values().stream()
                .map(CartItem::getSubtotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        boolean paid = false;
        if ("WALLET".equals(pm)) {
            WalletDAO w = new WalletDAO();
            boolean ok = w.payWithWallet(userId, total, "Order payment");
            if (!ok) {
                req.setAttribute("error", "Số dư ví không đủ.");
                doGet(req, resp);
                return;
            }
            paid = true;
        } else if ("GATEWAY".equals(pm)) {
            paid = true; // giả lập cổng thanh toán
        }

        // Build items
        java.util.List<OrderDAO.Item> items = new java.util.ArrayList<>();
        for (CartItem ci : cart.values()) {
            items.add(new OrderDAO.Item(ci.getProductId(), ci.getQuantity(), ci.getPrice()));
        }

        // Tạo đơn
        int orderId = new OrderDAO().createOrder(
                userId, fullName, phone, email, address, pm, paid, total, items
        );

        // Xoá giỏ
        s.setAttribute("cart", new LinkedHashMap<Integer, CartItem>());

        resp.sendRedirect(req.getContextPath() + "/orders/confirm?id=" + orderId);
    }

    private Integer uid(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) {
            return null;
        }
        Object v = s.getAttribute("uid");
        if (v instanceof Integer) {
            return (Integer) v;
        }
        if (v instanceof String) try {
            return Integer.valueOf((String) v);
        } catch (Exception ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, CartItem> getCart(HttpSession s) {
        Object o = s.getAttribute("cart");
        if (o instanceof Map) {
            return (Map<Integer, CartItem>) o;
        }
        Map<Integer, CartItem> m = new LinkedHashMap<>();
        s.setAttribute("cart", m);
        return m;
    }

    private static int parseInt(String s, int defVal) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return defVal;
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }
}
