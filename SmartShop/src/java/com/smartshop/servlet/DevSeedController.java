package com.smartshop.servlet;

import com.smartshop.dao.UserDAO;
import com.smartshop.model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet(urlPatterns={"/dev-seed"})
public class DevSeedController extends HttpServlet {
    private final UserDAO dao = new UserDAO();
    private static final String KEY = "letmein"; // đổi nếu muốn

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String key = req.getParameter("key");
        if (!KEY.equals(key)) { resp.sendError(403, "Forbidden"); return; }

        StringBuilder out = new StringBuilder();

        if (!dao.usernameExists("devmaster")) {
            User u = new User();
            u.setUsername("devmaster");
            u.setEmail("dev@local");
            u.setPhone("0000000000");
            u.setFullName("Developer");
            u.setRole("DEV");
            String err = dao.createWithError(u, "Dev@12345");
            out.append("DEV: ").append(err==null? "created" : "failed: "+err).append("\n");
        } else out.append("DEV: exists\n");

        if (!dao.usernameExists("admin")) {
            User u = new User();
            u.setUsername("admin");
            u.setEmail("admin@local");
            u.setPhone("0000000001");
            u.setFullName("Administrator");
            u.setRole("ADMIN");
            String err = dao.createWithError(u, "Admin@12345");
            out.append("ADMIN: ").append(err==null? "created" : "failed: "+err).append("\n");
        } else out.append("ADMIN: exists\n");

        resp.setContentType("text/plain; charset=UTF-8");
        resp.getWriter().print(out.toString());
    }
}
