package com.smartshop.servlet.wallet;

import com.smartshop.dao.WalletDAO;
import com.smartshop.dao.WalletDAO.TopupRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@WebServlet(urlPatterns = {"/wallet"})
public class WalletServlet extends HttpServlet {
    private static final String VIEW = "/WEB-INF/views/wallet/index.jsp";
    private static final String ADMIN_EMAIL = "smartshop.g5.prj301@gmail.com";
    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter FMT_VN = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(ZONE_VN);
    private static final NumberFormat CURRENCY_VN = NumberFormat.getCurrencyInstance(new Locale("vi","VN"));

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer userId = uid(req);

        try (WalletDAO wdao = new WalletDAO()) {
            req.setAttribute("balance", wdao.getBalance(userId));
        } catch (Exception e) {
            req.setAttribute("balance", new java.math.BigDecimal("0"));
            req.setAttribute("walletError", "Không lấy được số dư: " + e.getMessage());
        }

        HttpSession s = req.getSession(false);
        if (s != null) {
            copyFlash(s, req, "walletSuccess");
            copyFlash(s, req, "walletError");
            Object rid = s.getAttribute("walletTopupReqId");
            if (rid != null) {
                req.setAttribute("pendingTopupId", rid);
                req.setAttribute("pendingTopupAmount", s.getAttribute("walletTopupAmount"));
                Object expMs = s.getAttribute("walletTopupExpiresMs");
                if (expMs instanceof Long) {
                    String fmt = FMT_VN.format(Instant.ofEpochMilli((Long) expMs));
                    req.setAttribute("pendingTopupExpiresFmt", fmt);
                } else if (s.getAttribute("walletTopupExpiresFmt") != null) {
                    req.setAttribute("pendingTopupExpiresFmt", s.getAttribute("walletTopupExpiresFmt"));
                }
            }
        }

        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = nvl(req.getParameter("action"));
        if ("init_topup".equalsIgnoreCase(action)) { initTopup(req, resp); return; }
        if ("confirm_topup".equalsIgnoreCase(action)) { confirmTopup(req, resp); return; }
        resp.sendRedirect(req.getContextPath() + "/wallet");
    }

    private void initTopup(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession();
        Integer userId = uid(req);
        String amountStr = nvl(req.getParameter("amount"));
        try (WalletDAO wdao = new WalletDAO()) {
            BigDecimal amount = new BigDecimal(amountStr.trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Số tiền phải > 0");

            TopupRequest tr = wdao.createTopupRequest(userId, amount);

            s.setAttribute("walletTopupReqId", tr.id);
            s.setAttribute("walletTopupAmount", tr.amount);
            long expMs = tr.expiresAt.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
            s.setAttribute("walletTopupExpiresMs", expMs);
            s.setAttribute("walletTopupExpiresFmt", FMT_VN.format(Instant.ofEpochMilli(expMs)));

            String name = userDisplayName(s, userId);
            String amtFmt = CURRENCY_VN.format(tr.amount);
            String timeFmt = FMT_VN.format(tr.expiresAt.atZone(ZoneOffset.UTC));

            sendMail(ADMIN_EMAIL,
                "[SmartShop] Mã xác nhận nạp tiền",
                "Người dùng: <b>" + escape(name) + "</b><br/>" +
                "Số tiền: <b>" + amtFmt + "</b><br/>" +
                "Mã xác nhận: <b>" + tr.code + "</b><br/>" +
                "Hết hạn: <b>" + timeFmt + "</b>");

            s.setAttribute("walletSuccess", "Đã gửi mã xác nhận tới admin. Vui lòng nhập mã để hoàn tất.");
        } catch (Exception e) {
            s.setAttribute("walletError", "Tạo yêu cầu nạp thất bại: " + e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/wallet");
    }

    private void confirmTopup(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession();
        Integer userId = uid(req);
        String code = nvl(req.getParameter("code")).trim();
        long reqId = parseLong(nvl(req.getParameter("reqId")), -1);
        if (reqId < 0 && s.getAttribute("walletTopupReqId") != null) {
            reqId = ((Number) s.getAttribute("walletTopupReqId")).longValue();
        }
        try (WalletDAO wdao = new WalletDAO()) {
            wdao.confirmTopup(userId, reqId, code);
            s.setAttribute("walletSuccess", "Nạp tiền thành công.");
            s.removeAttribute("walletTopupReqId");
            s.removeAttribute("walletTopupAmount");
            s.removeAttribute("walletTopupExpiresMs");
            s.removeAttribute("walletTopupExpiresFmt");
        } catch (Exception e) {
            s.setAttribute("walletError", "Xác nhận thất bại: " + e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/wallet");
    }


    private void handleTopup(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession();
        Integer userId = uid(req);
        String amountStr = nvl(req.getParameter("amount"));
        try {
            BigDecimal amount = new BigDecimal(amountStr.trim());
            if (amount.scale() > 2) amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Số tiền phải > 0");
            try (WalletDAO wdao = new WalletDAO()) {
                wdao.addBalance(userId, amount, null, "User topup", "TOPUP");
            }
            s.setAttribute("walletSuccess", "Nạp " + amount + " thành công.");
        } catch (Exception e) {
            s.setAttribute("walletError", "Nạp thất bại: " + e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/wallet");
    }

    private Integer uid(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        Object o = s == null ? null : s.getAttribute("uid");
        if (o instanceof Integer) return (Integer) o;
        throw new RuntimeException("Please login");
    }
    private static String nvl(String s){ return s==null?"":s; }
    
    private static long parseLong(String s, long d){ try { return Long.parseLong(s); } catch(Exception e){ return d; } }

    private static void copyFlash(HttpSession s, HttpServletRequest req, String key){
        Object v = s.getAttribute(key);
        if (v != null) { req.setAttribute(key, v); s.removeAttribute(key); }
    }
    
    private String userDisplayName(HttpSession s, int userId){
        Object u = firstNonNull(s.getAttribute("user"), s.getAttribute("account"), s.getAttribute("acc"), s.getAttribute("authUser"));
        if (u instanceof Map){
            Map<?,?> m = (Map<?,?>) u;
            String full = str(m.get("full_name"), m.get("fullName"), m.get("fullname"));
            if (full != null && !full.isBlank()) return full;
            String name = str(m.get("name"));
            if (name != null && !name.isBlank()) return name;
        } else if (u != null){
            String full = callGetter(u, "getFullName", "getFullname");
            if (full != null && !full.isBlank()) return full;
            String name = callGetter(u, "getName");
            if (name != null && !name.isBlank()) return name;
        }
        return "User#" + userId;
    }
    private static Object firstNonNull(Object... xs){ for (Object x: xs) if (x!=null) return x; return null; }
    private static String str(Object... xs){ for (Object x: xs) if (x!=null) return String.valueOf(x); return null; }
    private static String callGetter(Object obj, String... getters){
        for (String g: getters) try{ Method m=obj.getClass().getMethod(g); Object v=m.invoke(obj); if (v!=null) return String.valueOf(v);}catch(Exception ignore){}
        return null;
    }
    private static String escape(String s){ return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }

    
    private void sendMail(String to, String subject, String html){
        try {
            // Dự án cũ thường có class này:
            Class<?> cls = Class.forName("com.smartshop.util.EmailUtil");
            cls.getMethod("send", String.class, String.class, String.class).invoke(null, to, subject, html);
        } catch(Exception ignore){
            // Nếu không có EmailUtil, có thể log ra console để dev biết
            System.out.println("[Email mock] to=" + to + " subj=" + subject + " body=" + html);
        }
    }
}
