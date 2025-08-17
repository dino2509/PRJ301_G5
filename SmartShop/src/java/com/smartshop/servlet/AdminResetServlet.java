package com.smartshop.servlet;

import com.smartshop.dao.UserDAO;
import com.smartshop.model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet(urlPatterns={"/admin-reset"})
public class AdminResetServlet extends HttpServlet {
    private final UserDAO dao = new UserDAO();
    private static final String KEY = "letmein"; // đổi sau khi dùng

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!KEY.equals(req.getParameter("key"))) { resp.sendError(403); return; }
        String newPass = req.getParameter("pass");
        if (newPass == null || newPass.length() < 6) { resp.sendError(400, "pass? >=6"); return; }
        User admin = dao.findByUsername("admin");
        if (admin == null) { resp.sendError(404, "no admin"); return; }
        boolean ok = dao.forceSetPassword(admin.getId(), newPass);
        resp.setContentType("text/plain; charset=UTF-8");
        resp.getWriter().print(ok ? "OK" : "FAIL");
    }
}
