package com.smartshop.servlet.auth;

import com.smartshop.dao.UserDAO;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;

@WebServlet(urlPatterns = "/register")
public class RegisterServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String email    = req.getParameter("email");
        String full     = req.getParameter("full_name");
        String phone    = req.getParameter("phone");
        String address    = req.getParameter("address");

        if (username==null || username.isBlank() || password==null || password.isBlank()) {
            req.setAttribute("error", "Thiếu username hoặc password");
            doGet(req, resp); return;
        }
        if (userDAO.usernameExists(username)) {
            req.setAttribute("error", "Username đã tồn tại");
            doGet(req, resp); return;
        }
        userDAO.create(username, email, full, phone, address, password, "USER");
        resp.sendRedirect(req.getContextPath() + "/login");
    }
}
