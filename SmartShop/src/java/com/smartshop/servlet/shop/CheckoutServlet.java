// src/main/java/com/smartshop/servlet/shop/CheckoutServlet.java
package com.smartshop.servlet.shop;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = {"/checkout"})
public class CheckoutServlet extends HttpServlet {

    private Integer uid(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return null;
        Object u = s.getAttribute("uid");
        if (u instanceof Integer) return (Integer) u;
        if (u instanceof String) {
            try { return Integer.valueOf((String) u); } catch (Exception ignored) {}
        }
        Object authUser = s.getAttribute("authUser");
        try {
            if (authUser != null) {
                return (Integer) authUser.getClass().getMethod("getId").invoke(authUser);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean ensureLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer uid = uid(req);
        if (uid == null) {
            String original = req.getRequestURI();
            String qs = req.getQueryString();
            if (qs != null && !qs.isEmpty()) original += "?" + qs;
            String next = URLEncoder.encode(original, StandardCharsets.UTF_8);
            resp.sendRedirect(req.getContextPath() + "/login?next=" + next);
            return false;
        }
        return true;
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!ensureLogin(req, resp)) return;

        // Lấy thông tin đã lưu trong session từ /account để prefill form
        HttpSession s = req.getSession();
        req.setAttribute("acc_fullName",  nvl(s.getAttribute("account_fullName")));
        req.setAttribute("acc_phone",     nvl(s.getAttribute("account_phone")));
        req.setAttribute("acc_email",     nvl(s.getAttribute("account_email")));
        req.setAttribute("acc_address",   nvl(s.getAttribute("account_address")));

        RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/views/shop/checkout.jsp");
        rd.forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!ensureLogin(req, resp)) return;

        String useAccount = req.getParameter("useAccount"); // "1" nếu dùng info account
        HttpSession s = req.getSession();

        String fullName, phone, email, address;
        if ("1".equals(useAccount)) {
            fullName = nvl(s.getAttribute("account_fullName"));
            phone    = nvl(s.getAttribute("account_phone"));
            email    = nvl(s.getAttribute("account_email"));
            address  = nvl(s.getAttribute("account_address"));
        } else {
            fullName = nvl(req.getParameter("fullName"));
            phone    = nvl(req.getParameter("phone"));
            email    = nvl(req.getParameter("email"));
            address  = nvl(req.getParameter("address"));
        }

        String payment = nvl(req.getParameter("paymentMethod")); // COD | WALLET | GATEWAY

        if (fullName.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            req.setAttribute("error", "Vui lòng nhập đủ họ tên, số điện thoại và địa chỉ.");
            req.setAttribute("acc_fullName", fullName);
            req.setAttribute("acc_phone", phone);
            req.setAttribute("acc_email", email);
            req.setAttribute("acc_address", address);
            RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/views/shop/checkout.jsp");
            rd.forward(req, resp);
            return;
        }

        // TODO: Tạo đơn hàng, lưu DB. Ở đây demo điều hướng theo phương thức thanh toán.
        switch (payment) {
            case "COD":
                resp.sendRedirect(req.getContextPath() + "/orders/confirm?pm=COD");
                return;
            case "WALLET":
                resp.sendRedirect(req.getContextPath() + "/wallet?action=pay");
                return;
            case "GATEWAY":
                resp.sendRedirect(req.getContextPath() + "/payment/fake?amount=cart");
                return;
            default:
                req.setAttribute("error", "Vui lòng chọn phương thức thanh toán.");
                RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/views/shop/checkout.jsp");
                rd.forward(req, resp);
        }
    }

    private String nvl(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
