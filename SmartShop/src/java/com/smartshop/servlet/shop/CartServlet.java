package com.smartshop.servlet.shop;

import com.smartshop.dao.ProductDAO;
import com.smartshop.dao.WalletDAO;
import com.smartshop.model.CartItem;
import com.smartshop.model.Product;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** /cart: giỏ hàng + đặt hàng COD/VÍ có mã xác nhận, trừ ví khi xác nhận, huỷ yêu cầu. */
@WebServlet(urlPatterns = {"/cart", "/cart/*"})
public class CartServlet extends HttpServlet {
    private static final String VIEW_CART = "/WEB-INF/views/shop/cart.jsp";
    private final ProductDAO productDao = new ProductDAO();

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter FMT_VN = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(ZONE_VN);

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

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { handle(req, resp); }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { handle(req, resp); }

    private void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String info = req.getPathInfo(); // null, "/", "/add", "/update", "/remove", "/place", "/confirm", "/cancel"
        if (info == null || "/".equals(info)) {
            if (isUpdate(req))       info = "/update";
            else if (isRemove(req))  info = "/remove";
            else if (isAdd(req))     info = "/add";
            else if (isPlace(req))   info = "/place";
            else if (isConfirm(req)) info = "/confirm";
            else if (isCancel(req))  info = "/cancel";
            else                     info = "/";
        }

        HttpSession session = req.getSession();
        Map<Integer, Object> cart = getRawCart(session);

        if ("/".equals(info)) {
            req.setAttribute("cartItems", buildViewItems(cart));
            pushAcc(req);

            try (WalletDAO w = new WalletDAO()) {
                Integer uid = uid(req);
                req.setAttribute("walletBalance", w.getBalance(uid));
            } catch (Exception ignore) { req.setAttribute("walletBalance", new BigDecimal("0")); }

            copyFlash(session, req, "cartSuccess");
            copyFlash(session, req, "cartError");
            if (session.getAttribute("coReqId") != null) {
                req.setAttribute("pendingCheckoutId", session.getAttribute("coReqId"));
                req.setAttribute("pendingTotalFmt", session.getAttribute("coTotalFmt"));
                req.setAttribute("pendingExpiresFmt", session.getAttribute("coExpiresFmt"));
                req.setAttribute("pendingMethod", session.getAttribute("coMethod"));
            }
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
                    int current = ci.getQuantity() > 0 ? ci.getQuantity() : ci.getQty();
                    int newQty = current + (qty <= 0 ? 1 : qty);
                    ci.setQuantity(newQty);
                    ci.setQty(newQty);
                    ci.setName(p.getName()); ci.setPrice(p.getPrice()); ci.setImageUrl(p.getImageUrl()); ci.setProductId(p.getId());
                } else if (cur instanceof CartLine) {
                    ((CartLine) cur).qty += (qty <= 0 ? 1 : qty);
                } else {
                    CartItem item = new CartItem();
                    item.setProductId(p.getId());
                    item.setName(p.getName());
                    item.setPrice(p.getPrice());
                    item.setImageUrl(p.getImageUrl());
                    item.setQuantity(qty <= 0 ? 1 : qty);
                    item.setQty(qty <= 0 ? 1 : qty);
                    cart.put(pid, item);
                }
                break;
            }
            case "/update": {
                if (pid >= 0) {
                    Object cur = cart.get(pid);
                    int newQty = Math.max(1, qty);
                    if (cur instanceof CartItem) {
                        CartItem it = (CartItem) cur; it.setQuantity(newQty); it.setQty(newQty);
                    } else if (cur instanceof CartLine) {
                        ((CartLine) cur).qty = newQty;
                    }
                }
                break;
            }
            case "/remove": {
                if (pid >= 0) cart.remove(pid);
                break;
            }
            case "/place": {
                placeOrder(req, resp, cart);
                return;
            }
            case "/confirm": {
                confirmOrder(req, resp, cart);
                return;
            }
            case "/cancel": {
                cancelOrder(req, resp);
                return;
            }
            default: break;
        }
        resp.sendRedirect(req.getContextPath() + "/cart");
    }

    /* ---------------- Place / Confirm / Cancel ---------------- */

    private void placeOrder(HttpServletRequest req, HttpServletResponse resp, Map<Integer, Object> cart) throws IOException {
        HttpSession s = req.getSession();
        Integer userId = uid(req);

        String sel = nvl(req.getParameter("selectedIds")); // "1,3,5"
        String pm = nvl(req.getParameter("pm")).toUpperCase(Locale.ROOT); // COD | WALLET
        String fullName = nvl(req.getParameter("fullName"));
        String phone    = nvl(req.getParameter("phone"));
        String email    = nvl(req.getParameter("email"));
        String address  = nvl(req.getParameter("address"));

        if (sel.isBlank()) { s.setAttribute("cartError", "Chưa chọn sản phẩm."); resp.sendRedirect(req.getContextPath()+"/cart"); return; }
        if (pm.isBlank())  { s.setAttribute("cartError", "Chưa chọn phương thức thanh toán."); resp.sendRedirect(req.getContextPath()+"/cart"); return; }

        Set<Integer> selected = parseIdSet(sel);
        BigDecimal total = BigDecimal.ZERO;
        List<Map<String,Object>> items = new ArrayList<>();

        for (Map.Entry<Integer,Object> e : cart.entrySet()) {
            int id = e.getKey();
            if (!selected.contains(id)) continue;
            int q = readQtyFromAny(e.getValue());
            Product p = productDao.findById(id);
            if (p == null) continue;
            BigDecimal price = p.getPrice() == null ? BigDecimal.ZERO : p.getPrice();
            BigDecimal line = price.multiply(BigDecimal.valueOf(q));
            total = total.add(line);
            Map<String,Object> it = new LinkedHashMap<>();
            it.put("product_id", id);
            it.put("name", p.getName());
            it.put("price", price);
            it.put("qty", q);
            items.add(it);
        }
        total = total.setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) <= 0) { s.setAttribute("cartError", "Tổng tiền không hợp lệ."); resp.sendRedirect(req.getContextPath()+"/cart"); return; }

        // CHẶN TỪ SERVER: nếu chọn WALLET mà số dư < tổng thì báo lỗi và không tạo yêu cầu
        if ("WALLET".equalsIgnoreCase(pm)) {
            try (WalletDAO w = new WalletDAO()) {
                BigDecimal bal = w.getBalance(userId);
                if (bal.compareTo(total) < 0) {
                    s.setAttribute("cartError", "Số dư ví không đủ. Cần " + fmtVND(total) + ", còn " + fmtVND(bal) + ".");
                    resp.sendRedirect(req.getContextPath()+"/cart");
                    return;
                }
            } catch (Exception e) {
                s.setAttribute("cartError", "Không đọc được số dư ví: " + e.getMessage());
                resp.sendRedirect(req.getContextPath()+"/cart");
                return;
            }
        }

        try (CheckoutDAO co = new CheckoutDAO()) {
            Request r = co.create(userId, items, total, pm, fullName, phone, email, address);
            s.setAttribute("coReqId", r.id);
            s.setAttribute("coTotalFmt", r.totalFmtVN());
            s.setAttribute("coExpiresFmt", r.expiresFmtVN());
            s.setAttribute("coMethod", pm);

            String body = "Xin chào <b>" + escapeHtml(fullName.isBlank()? userDisplayName(s, userId) : fullName) + "</b><br/>" +
                          "Mã xác nhận " + (pm.equals("COD") ? "đặt hàng" : "thanh toán") + ": <b>"+ r.code +"</b><br/>" +
                          "Tổng tiền: <b>"+ r.totalFmtVN() +"</b><br/>" +
                          "Hết hạn: <b>"+ r.expiresFmtVN() +"</b>";
            sendMail(emailOrFallback(s, email, userId), "[SmartShop] Xác nhận " + (pm.equals("COD")?"đặt hàng":"thanh toán"), body);
            s.setAttribute("cartSuccess", "Đã gửi mã xác nhận tới email. Vui lòng nhập mã để hoàn tất.");
        } catch (Exception ex) {
            s.setAttribute("cartError", "Không tạo được yêu cầu: " + ex.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/cart");
    }

    private void confirmOrder(HttpServletRequest req, HttpServletResponse resp, Map<Integer, Object> cart) throws IOException {
        HttpSession s = req.getSession();
        Integer userId = uid(req);
        long reqId = parseLong(nvl(req.getParameter("reqId")), -1);
        String code = nvl(req.getParameter("code")).trim();
        if (reqId < 0) { s.setAttribute("cartError", "Thiếu yêu cầu cần xác nhận."); resp.sendRedirect(req.getContextPath()+"/cart"); return; }

        try (CheckoutDAO co = new CheckoutDAO()) {
            Request r = co.find(reqId, userId);
            if (r == null) throw new SQLException("Yêu cầu không tồn tại");
            if (!"PENDING".equalsIgnoreCase(r.status)) throw new SQLException("Yêu cầu đã " + r.status);
            if (r.expiresAt.isBefore(Instant.now())) throw new SQLException("Mã đã hết hạn");
            if (!r.code.equalsIgnoreCase(code)) throw new SQLException("Mã không đúng");

            if ("WALLET".equalsIgnoreCase(r.method)) {
                try (WalletDAO w = new WalletDAO()) {
                    BigDecimal bal = w.getBalance(userId);
                    if (bal.compareTo(r.total) < 0) throw new SQLException("Số dư không đủ");
                    w.addBalance(userId, r.total.negate(), r.id, "Pay order " + r.id, "PURCHASE");
                }
                co.insertTransaction(userId, null, "PURCHASE", r.total, "WALLET", "SUCCESS");
                co.markUsed(r.id);
                removePurchasedItems(cart, r.items);
                s.setAttribute("cartSuccess", "Thanh toán thành công bằng ví.");
            } else {
                co.insertTransaction(userId, null, "PURCHASE", r.total, "COD", "PENDING");
                co.markUsed(r.id);
                removePurchasedItems(cart, r.items);
                s.setAttribute("cartSuccess", "Đặt hàng thành công. Đơn ở trạng thái chờ giao.");
            }
            clearPending(s);
        } catch (Exception ex) {
            s.setAttribute("cartError", "Xác nhận thất bại: " + ex.getMessage());
        }
        resp.sendRedirect(req.getContextPath()+"/cart");
    }

    private void cancelOrder(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession();
        Integer userId = uid(req);
        long reqId = parseLong(nvl(req.getParameter("reqId")), -1);
        if (reqId < 0) { s.setAttribute("cartError", "Không có yêu cầu để huỷ."); resp.sendRedirect(req.getContextPath()+"/cart"); return; }
        try (CheckoutDAO co = new CheckoutDAO()) {
            Request r = co.find(reqId, userId);
            if (r == null) throw new SQLException("Yêu cầu không tồn tại");
            if (!"PENDING".equalsIgnoreCase(r.status)) throw new SQLException("Không thể huỷ. Yêu cầu đã " + r.status);
            co.markCancelled(reqId);
            s.setAttribute("cartSuccess", "Đã huỷ yêu cầu hiện tại.");
            clearPending(s);
        } catch (Exception e) {
            s.setAttribute("cartError", "Huỷ thất bại: " + e.getMessage());
        }
        resp.sendRedirect(req.getContextPath()+"/cart");
    }

    private void clearPending(HttpSession s){
        s.removeAttribute("coReqId");
        s.removeAttribute("coTotalFmt");
        s.removeAttribute("coExpiresFmt");
        s.removeAttribute("coMethod");
    }

    /* ---------------- Render helpers ---------------- */

    private List<ViewItem> buildViewItems(Map<Integer, Object> cart){
        List<ViewItem> list = new ArrayList<>();
        if (cart == null) return list;
        for (Map.Entry<Integer,Object> e : cart.entrySet()) {
            int pid = e.getKey();
            int qty = readQtyFromAny(e.getValue());
            Product p = productDao.findById(pid);
            if (p == null) p = new Product();
            p.setId(pid);
            list.add(new ViewItem(p, qty));
        }
        return list;
    }

    private int readQtyFromAny(Object o){
        if (o instanceof CartItem) {
            CartItem it = (CartItem) o;
            return it.getQuantity() > 0 ? it.getQuantity() : Math.max(1, it.getQty());
        } else if (o instanceof CartLine) {
            return Math.max(1, ((CartLine) o).qty);
        } else return 1;
    }

    /** Xoá các sản phẩm đã mua khỏi giỏ. */
    private void removePurchasedItems(Map<Integer,Object> cart, List<Item> items){
        if (cart == null || items == null) return;
        for (Item it : items) cart.remove(it.productId);
    }

    /** Đẩy thông tin account triệt để. */
    private void pushAcc(HttpServletRequest req){
        HttpSession s = req.getSession(false);
        Integer uid = null;
        if (s != null && s.getAttribute("uid") instanceof Integer) uid = (Integer) s.getAttribute("uid");

        String fullName=null, phone=null, email=null, address=null;

        Object user = s==null?null:firstNonNull(s.getAttribute("user"), s.getAttribute("account"), s.getAttribute("acc"), s.getAttribute("authUser"));
        if (user instanceof Map) {
            Map<?,?> m = (Map<?,?>) user;
            fullName = str(m.get("full_name"), m.get("fullName"), m.get("fullname"), m.get("name"));
            phone    = str(m.get("phone"), m.get("mobile"), m.get("tel"));
            email    = str(m.get("email"));
            address  = str(m.get("address"), m.get("addr"));
        } else if (user != null) {
            fullName = callGetter(user, "getFullName", "getFullname", "getName");
            phone    = callGetter(user, "getPhone", "getMobile", "getTel");
            email    = callGetter(user, "getEmail");
            address  = callGetter(user, "getAddress", "getAddr");
        }

        if (uid != null && (isBlank(fullName) || isBlank(email) || isBlank(phone) || isBlank(address))) {
            try (Connection con = com.smartshop.util.DB.getConnection()) {
                UserRow ur = findUserFromDB(con, uid);
                if (ur != null) {
                    if (isBlank(fullName)) fullName = ur.fullName;
                    if (isBlank(phone))    phone    = ur.phone;
                    if (isBlank(email))    email    = ur.email;
                    if (isBlank(address))  address  = ur.address;
                }
            } catch (Exception ignore) {}
        }

        req.setAttribute("accFullName", emptyToNull(fullName));
        req.setAttribute("accPhone",    emptyToNull(phone));
        req.setAttribute("accEmail",    emptyToNull(email));
        req.setAttribute("accAddress",  emptyToNull(address));
    }

    private UserRow findUserFromDB(Connection con, int uid) throws SQLException {
        String[] tables = {"Users","User","Accounts","Account","Customers","Customer","KhachHang"};
        for (String t : tables) {
            String idCol = firstExistingCol(con, t, new String[]{"id","user_id","UserId","UID","account_id","AccountId","customer_id","CustomerId","MaKH"});
            if (idCol == null) continue;
            String nameCol = firstExistingCol(con, t, new String[]{"full_name","fullname","FullName","name","Name","username","Username"});
            String phoneCol= firstExistingCol(con, t, new String[]{"phone","mobile","tel","Phone","PhoneNumber","SoDienThoai","SDT"});
            String emailCol= firstExistingCol(con, t, new String[]{"email","Email"});
            String addrCol = firstExistingCol(con, t, new String[]{"address","addr","Address","DiaChi"});
            if (nameCol==null && phoneCol==null && emailCol==null && addrCol==null) continue;

            StringBuilder sql = new StringBuilder("SELECT TOP 1 ");
            boolean first=true;
            if (nameCol != null){ sql.append(nameCol).append(" AS full_name"); first=false; }
            if (phoneCol!= null){ sql.append(first?"":",").append(phoneCol).append(" AS phone"); first=false; }
            if (emailCol!= null){ sql.append(first?"":",").append(emailCol).append(" AS email"); first=false; }
            if (addrCol != null){ sql.append(first?"":",").append(addrCol).append(" AS address"); }
            sql.append(" FROM ").append(t).append(" WHERE ").append(idCol).append("=?");

            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                ps.setInt(1, uid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        UserRow u = new UserRow();
                        safeSet(() -> u.fullName = rs.getString("full_name"));
                        safeSet(() -> u.phone    = rs.getString("phone"));
                        safeSet(() -> u.email    = rs.getString("email"));
                        safeSet(() -> u.address  = rs.getString("address"));
                        return u;
                    }
                }
            } catch (SQLException ignore) {}
        }
        return null;
    }
    private interface SqlRunnable { void run() throws SQLException; }
    private static void safeSet(SqlRunnable r){ try{ r.run(); }catch(Exception ignore){} }
    private static String firstExistingCol(Connection con, String table, String[] candidates) throws SQLException {
        for (String c : candidates) {
            String sql = "SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(?) AND name = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, table);
                ps.setString(2, c);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return c;
                }
            }
        }
        return null;
    }
    private static class UserRow { String fullName, phone, email, address; }

    /* ---------------- small utils ---------------- */

    private boolean isAdd(HttpServletRequest req)     { String a = lower(pickFirst(req,"action","op","act","btn","submit")); return "add".equals(a) || hasParam(req,"add") || hasParam(req,"addToCart"); }
    private boolean isUpdate(HttpServletRequest req)  { String a = lower(pickFirst(req,"action","op","act","btn","submit")); return "update".equals(a) || hasParam(req,"update"); }
    private boolean isRemove(HttpServletRequest req)  { String a = lower(pickFirst(req,"action","op","act","btn","submit")); return "remove".equals(a) || hasParam(req,"remove") || hasParam(req,"delete"); }
    private boolean isPlace(HttpServletRequest req)   { String a = lower(pickFirst(req,"action","op","act","btn","submit")); return "place_order".equals(a); }
    private boolean isConfirm(HttpServletRequest req) { String a = lower(pickFirst(req,"action","op","act","btn","submit")); return "confirm_order".equals(a); }
    private boolean isCancel(HttpServletRequest req)  { String a = lower(pickFirst(req,"action","op","act","btn","submit")); return "cancel_order".equals(a); }

    private int readProductId(HttpServletRequest req) { String v = pickFirst(req, "id", "productId", "pid", "product_id", "pId", "prodId"); return parseInt(v, -1); }
    private int readQty(HttpServletRequest req)       { String v = pickFirst(req, "qty", "quantity", "q"); int q = parseInt(v, 1); return q <= 0 ? 1 : q; }
    private String pickFirst(HttpServletRequest req, String... names){ for (String n : names){ String v = req.getParameter(n); if (v != null && !v.isBlank()) return v; } return null; }
    private boolean hasParam(HttpServletRequest req, String name) { return req.getParameter(name) != null; }
    private String lower(String s) { return s == null ? null : s.toLowerCase(); }
    private int parseInt(String s, int defVal) { try { return Integer.parseInt(s); } catch (Exception e) { return defVal; } }
    private static long parseLong(String s, long d){ try { return Long.parseLong(s); } catch(Exception e){ return d; } }
    private static String nvl(String s){ return s==null?"":s; }
    private static boolean isBlank(String s){ return s==null || s.isBlank(); }
    private static String emptyToNull(String s){ return isBlank(s)?null:s; }
    private static Object firstNonNull(Object... xs){ for (Object x: xs) if (x!=null) return x; return null; }
    private static String str(Object... xs){ for (Object x: xs) if (x!=null) return String.valueOf(x); return null; }
    private static String callGetter(Object obj, String... getters){
        for (String g: getters) try{ Method m=obj.getClass().getMethod(g); Object v=m.invoke(obj); if (v!=null) return String.valueOf(v);}catch(Exception ignore){}
        return null;
    }
    private static Integer uid(HttpServletRequest req){
        HttpSession s = req.getSession(false);
        Object o = s == null ? null : s.getAttribute("uid");
        if (o instanceof Integer) return (Integer) o;
        throw new RuntimeException("Please login");
    }
    private static Set<Integer> parseIdSet(String csv){
        Set<Integer> set = new LinkedHashSet<>();
        for (String t : csv.split(",")) {
            try { set.add(Integer.parseInt(t.trim())); } catch (Exception ignore) {}
        }
        return set;
    }
    private static void copyFlash(HttpSession s, HttpServletRequest req, String key){
        Object v = s.getAttribute(key);
        if (v != null) { req.setAttribute(key, v); s.removeAttribute(key); }
    }
    private static String emailOrFallback(HttpSession s, String prefer, int userId){
        if (!isBlank(prefer)) return prefer;
        Object u = firstNonNull(s.getAttribute("user"), s.getAttribute("account"), s.getAttribute("acc"), s.getAttribute("authUser"));
        if (u instanceof Map) return str(((Map<?,?>)u).get("email"));
        if (u != null) { String e = callGetter(u, "getEmail"); if (!isBlank(e)) return e; }
        return "no-reply@example.com";
    }
    private static String userDisplayName(HttpSession s, int userId){
        Object u = firstNonNull(s.getAttribute("user"), s.getAttribute("account"), s.getAttribute("acc"), s.getAttribute("authUser"));
        if (u instanceof Map){
            Map<?,?> m = (Map<?,?>) u;
            String full = str(m.get("full_name"), m.get("fullName"), m.get("fullname"));
            if (!isBlank(full)) return full;
            String name = str(m.get("name"));
            if (!isBlank(name)) return name;
        } else if (u != null){
            String full = callGetter(u, "getFullName", "getFullname");
            if (!isBlank(full)) return full;
            String name = callGetter(u, "getName");
            if (!isBlank(name)) return name;
        }
        return "User#" + userId;
    }

    static String escapeHtml(String s){ return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
    static String escapeJson(String s){ return s==null?"":s.replace("\\","\\\\").replace("\"","\\\""); }
    private static String fmtVND(BigDecimal x){ java.text.NumberFormat f = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi","VN")); return f.format(x==null?BigDecimal.ZERO:x); }

    private void sendMail(String to, String subject, String html){
        try {
            Class<?> cls = Class.forName("com.smartshop.util.EmailUtil");
            cls.getMethod("send", String.class, String.class, String.class).invoke(null, to, subject, html);
        } catch(Exception ignore){
            System.out.println("[Email mock] to=" + to + " subj=" + subject + " body=" + html);
        }
    }

    /* ---------------- DTOs ---------------- */
    static class CartLine { Product product; int qty; CartLine(Product p, int q){ this.product=p; this.qty=q; } }
    public static class ViewItem {
        private final Product product; private final int qty;
        public ViewItem(Product p, int q){ this.product=p; this.qty=q; }
        public Product getProduct(){ return product; }
        public int getQty(){ return qty; }
    }
    static class Item { int productId; int qty; }

    /* ---------------- CheckoutDAO ---------------- */
    static class CheckoutDAO implements AutoCloseable {
        private final Connection con;
        CheckoutDAO() throws SQLException { this.con = com.smartshop.util.DB.getConnection(); ensureTables(); }
        @Override public void close() { try { con.close(); } catch (Exception ignore) {} }

        private void ensureTables() throws SQLException {
            try (Statement st = con.createStatement()) {
                st.execute("""
                  IF OBJECT_ID('dbo.CheckoutRequests','U') IS NULL
                  BEGIN
                    CREATE TABLE dbo.CheckoutRequests(
                      id BIGINT IDENTITY(1,1) PRIMARY KEY,
                      user_id INT NOT NULL,
                      items NVARCHAR(MAX) NOT NULL,
                      total_amount DECIMAL(19,2) NOT NULL,
                      method NVARCHAR(16) NOT NULL, -- COD/WALLET
                      code NVARCHAR(16) NOT NULL,
                      status NVARCHAR(16) NOT NULL DEFAULT 'PENDING',
                      expires_at DATETIME2 NOT NULL,
                      full_name NVARCHAR(255) NULL,
                      phone NVARCHAR(64) NULL,
                      email NVARCHAR(255) NULL,
                      address NVARCHAR(255) NULL,
                      created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
                    );
                  END
                """);
                st.execute("""
                  IF OBJECT_ID('dbo.Transactions','U') IS NULL
                  BEGIN
                    CREATE TABLE dbo.Transactions(
                      transaction_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                      transaction_code NVARCHAR(32) NOT NULL,
                      user_id INT NOT NULL,
                      order_id BIGINT NULL,
                      type NVARCHAR(20) NOT NULL,
                      amount DECIMAL(19,2) NOT NULL,
                      method NVARCHAR(32) NOT NULL,
                      status NVARCHAR(20) NOT NULL,
                      created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
                    );
                    CREATE UNIQUE INDEX UX_Transactions_Code ON dbo.Transactions(transaction_code);
                  END
                """);
            }
        }

        Request create(int userId, List<Map<String,Object>> items, BigDecimal total, String method,
                       String fullName, String phone, String email, String address) throws SQLException {
            String code = genCode();
            String itemsJson = toJson(items);
            long id;
            try (PreparedStatement ps = con.prepareStatement("""
                INSERT INTO dbo.CheckoutRequests(user_id, items, total_amount, method, code, expires_at, full_name, phone, email, address)
                VALUES (?,?,?,?,?, DATEADD(minute,5,SYSUTCDATETIME()), ?,?,?,?);
                SELECT SCOPE_IDENTITY();
            """)) {
                ps.setInt(1, userId);
                ps.setString(2, itemsJson);
                ps.setBigDecimal(3, total);
                ps.setString(4, method);
                ps.setString(5, code);
                ps.setString(6, fullName);
                ps.setString(7, phone);
                ps.setString(8, email);
                ps.setString(9, address);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); id = rs.getLong(1); }
            }
            Instant exp;
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            try (PreparedStatement ps = con.prepareStatement("SELECT expires_at FROM dbo.CheckoutRequests WHERE id=?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); exp = rs.getTimestamp(1, utc).toInstant(); }
            }
            return new Request(id, userId, total, method, code, "PENDING", exp, parseItems(itemsJson));
        }

        Request find(long id, int userId) throws SQLException {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT user_id, total_amount, method, code, status, expires_at, items FROM dbo.CheckoutRequests WHERE id=? AND user_id=?")) {
                ps.setLong(1, id); ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    int uid = rs.getInt(1);
                    BigDecimal total = rs.getBigDecimal(2);
                    String method = rs.getString(3);
                    String code = rs.getString(4);
                    String status = rs.getString(5);
                    Instant exp = rs.getTimestamp(6, utc).toInstant();
                    String itemsJson = rs.getString(7);
                    return new Request(id, uid, total, method, code, status, exp, parseItems(itemsJson));
                }
            }
        }

        void markUsed(long id) throws SQLException {
            try (PreparedStatement ps = con.prepareStatement("UPDATE dbo.CheckoutRequests SET status='USED' WHERE id=?")) {
                ps.setLong(1, id); ps.executeUpdate();
            }
        }
        void markCancelled(long id) throws SQLException {
            try (PreparedStatement ps = con.prepareStatement("UPDATE dbo.CheckoutRequests SET status='CANCELLED' WHERE id=?")) {
                ps.setLong(1, id); ps.executeUpdate();
            }
        }

        void insertTransaction(int userId, Long orderId, String type, BigDecimal amount, String method, String status) throws SQLException {
            String code = genTxCode(type.equals("PURCHASE")?"OD":"TX");
            try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO dbo.Transactions(transaction_code,user_id,order_id,type,amount,method,status) VALUES(?,?,?,?,?,?,?)")) {
                ps.setString(1, code);
                ps.setInt(2, userId);
                if (orderId == null) ps.setNull(3, Types.BIGINT); else ps.setLong(3, orderId);
                ps.setString(4, type);
                ps.setBigDecimal(5, amount);
                ps.setString(6, method);
                ps.setString(7, status);
                ps.executeUpdate();
            }
        }

        private static String genCode() {
            String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
            StringBuilder sb = new StringBuilder(6);
            java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
            for (int i=0;i<6;i++) sb.append(chars.charAt(r.nextInt(chars.length())));
            return sb.toString();
        }
        private static String genTxCode(String prefix){
            String ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
            int rnd = java.util.concurrent.ThreadLocalRandom.current().nextInt(100,999);
            return prefix + "-" + ts + "-" + rnd;
        }
        private static String toJson(List<Map<String,Object>> items){
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Map<String,Object> m : items){
                if (!first) sb.append(',');
                first=false;
                sb.append('{')
                  .append("\"product_id\":").append(m.get("product_id"))
                  .append(",\"name\":").append('"').append(escapeJson(String.valueOf(m.get("name")))).append('"')
                  .append(",\"price\":").append(m.get("price"))
                  .append(",\"qty\":").append(m.get("qty"))
                  .append('}');
            }
            sb.append(']');
            return sb.toString();
        }
        private static List<Item> parseItems(String json){
            List<Item> list = new ArrayList<>();
            if (json == null) return list;
            String[] parts = json.split("\\{");
            for (String p : parts){
                if (!p.contains("product_id")) continue;
                Item it = new Item();
                it.productId = valInt(p, "\"product_id\":");
                it.qty       = valInt(p, "\"qty\":");
                if (it.productId > 0 && it.qty > 0) list.add(it);
            }
            return list;
        }
        private static int valInt(String s, String key){
            int i = s.indexOf(key);
            if (i<0) return -1;
            i += key.length();
            int j=i;
            while (j<s.length() && Character.isDigit(s.charAt(j))) j++;
            try { return Integer.parseInt(s.substring(i,j)); } catch(Exception e){ return -1; }
        }
    }

    static class Request {
        final long id; final int userId; final BigDecimal total; final String method; final String code; final String status; final Instant expiresAt; final List<Item> items;
        Request(long id, int userId, BigDecimal total, String method, String code, String status, Instant exp, List<Item> items){
            this.id=id; this.userId=userId; this.total=total; this.method=method; this.code=code; this.status=status; this.expiresAt=exp; this.items=items;
        }
        String totalFmtVN(){ java.text.NumberFormat f = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi","VN")); return f.format(total); }
        String expiresFmtVN(){ return FMT_VN.format(expiresAt); }
    }
}
