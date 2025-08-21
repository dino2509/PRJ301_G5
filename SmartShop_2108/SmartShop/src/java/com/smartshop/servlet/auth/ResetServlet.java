package com.smartshop.servlet.auth;

import com.smartshop.dao.UserDAO;
import com.smartshop.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet(urlPatterns = "/reset")
public class ResetServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        String email = req.getParameter("email");

        // bắt buộc phải có cả token và email, thiếu -> quay về /forgot
        if (isBlank(token) || isBlank(email)) {
            resp.sendRedirect(req.getContextPath() + "/forgot");
            return;
        }

        User u = userDAO.findByResetTokenAndEmail(token, email);
        if (u == null) {
            req.getRequestDispatcher("/WEB-INF/views/auth/reset_expired.jsp").forward(req, resp);
            return;
        }

        // hợp lệ -> hiển thị form với email cố định
        req.setAttribute("resetEmail", u.getEmail());
        req.setAttribute("token", token);
        req.getRequestDispatcher("/WEB-INF/views/auth/reset.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        String email = req.getParameter("email");
        String pw = req.getParameter("password");
        String cf = req.getParameter("confirm");

        if (isBlank(token) || isBlank(email)) {
            resp.sendRedirect(req.getContextPath() + "/forgot");
            return;
        }

        User u = userDAO.findByResetTokenAndEmail(token, email);
        if (u == null) {
            req.getRequestDispatcher("/WEB-INF/views/auth/reset_expired.jsp").forward(req, resp);
            return;
        }

        if (isBlank(pw) || !pw.equals(cf)) {
            req.setAttribute("resetEmail", u.getEmail());
            req.setAttribute("token", token);
            req.setAttribute("error", "Mật khẩu không hợp lệ hoặc không khớp.");
            req.getRequestDispatcher("/WEB-INF/views/auth/reset.jsp").forward(req, resp);
            return;
        }

        userDAO.changePassword(u.getId(), pw); // đổi mật khẩu và hủy token
        resp.sendRedirect(req.getContextPath() + "/login?msg=reset_ok");
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
