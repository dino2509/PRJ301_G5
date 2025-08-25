// src/main/java/com/smartshop/servlet/shop/CheckoutServlet.java
package com.smartshop.servlet.shop;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@WebServlet(urlPatterns = {"/checkout"})
public class CheckoutServlet extends HttpServlet {

    private Integer uid(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return null;
        Object u = s.getAttribute("uid");
        if (u instanceof Integer) return (Integer) u;
        if (u instanceof String) { try { return Integer.valueOf((String) u); } catch (Exception ignored) {} }
        Object authUser = s.getAttribute("authUser");
        try { if (authUser != null) return (Integer) authUser.getClass().getMethod("getId").invoke(authUser); }
        catch (Exception ignored) {}
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
        HttpSession s = req.getSession();
        req.setAttribute("acc_fullName",  nvl(s.getAttribute("account_fullName")));
        req.setAttribute("acc_phone",     nvl(s.getAttribute("account_phone")));
        req.setAttribute("acc_email",     nvl(s.getAttribute("account_email")));
        req.setAttribute("acc_address",   nvl(s.getAttribute("account_address")));
        RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/views/shop/checkout_form.jsp");
        rd.forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureLogin(req, resp)) return;

        String[] pids = req.getParameterValues("pid");
        String[] qtys = req.getParameterValues("qty");
        if (pids == null || qtys == null || pids.length == 0 || pids.length != qtys.length) {
            req.getSession().setAttribute("flashError", "Không có sản phẩm hợp lệ.");
            resp.sendRedirect(req.getContextPath() + "/cart");
            return;
        }

        String payMethod = Optional.ofNullable(req.getParameter("payMethod")).orElse("cod");
        Map<Integer, Integer> selection = new LinkedHashMap<>();
        for (int i = 0; i < pids.length; i++) {
            try {
                int pid = Integer.parseInt(pids[i]);
                int q = Math.max(1, Integer.parseInt(qtys[i]));
                selection.put(pid, q);
            } catch (NumberFormatException ignore) {}
        }
        if (selection.isEmpty()) {
            req.getSession().setAttribute("flashError", "Không có sản phẩm hợp lệ.");
            resp.sendRedirect(req.getContextPath() + "/cart");
            return;
        }

        req.getSession().setAttribute("checkout.selection", selection);
        req.getSession().setAttribute("checkout.payMethod", payMethod);
        req.getRequestDispatcher("/WEB-INF/views/shop/checkout_confirm.jsp").forward(req, resp);
    }

    private String nvl(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
}
