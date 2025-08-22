// src/main/java/com/smartshop/servlet/account/AccountServlet.java
package com.smartshop.servlet.account;

import com.smartshop.dao.UserDAO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet(urlPatterns = {"/account"})
public class AccountServlet extends HttpServlet {

    private Integer uid(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return null;
        Object v = s.getAttribute("uid");
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof String) try { return Integer.valueOf((String)v); } catch(Exception ignored){}
        return null;
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer userId = uid(req);
        if (userId == null) { resp.sendRedirect(req.getContextPath()+"/login?next=/account"); return; }

        UserDAO.Profile p = new UserDAO().getProfile(userId);
        req.setAttribute("profile", p);
        RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/views/account/account.jsp");
        rd.forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer userId = uid(req);
        if (userId == null) { resp.sendRedirect(req.getContextPath()+"/login?next=/account"); return; }

        String address = req.getParameter("address");
        new UserDAO().updateAddress(userId, address);

        // Lưu nhanh vào session cho các nơi khác dùng
        HttpSession s = req.getSession();
        s.setAttribute("account_address", address);

        resp.sendRedirect(req.getContextPath()+"/account?saved=1");
    }
}
