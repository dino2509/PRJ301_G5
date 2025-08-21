package com.smartshop.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;

public class AdminController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureAdmin(req, resp)) return;

        String servlet = req.getServletPath();     // "/admin"
        String path = req.getPathInfo();           // null, "/products", "/users", ...

        if ("/admin".equals(servlet) && (path == null || "/".equals(path))) {
            resp.sendRedirect(req.getContextPath() + "/admin/products");
            return;
        }

        if ("/admin".equals(servlet)) {
            if ("/products".equals(path)) {
                req.getRequestDispatcher("/WEB-INF/views/admin/products.jsp").forward(req, resp);
                return;
            }
            if ("/users".equals(path)) {
                req.getRequestDispatcher("/WEB-INF/views/admin/users.jsp").forward(req, resp);
                return;
            }
        }
        resp.sendError(404);
    }

    private boolean ensureAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        Boolean isAdmin = s != null ? (Boolean) s.getAttribute("isAdmin") : null;
        if (isAdmin != null && isAdmin) return true;

        // lưu trang đích để quay lại sau đăng nhập
        req.getSession(true).setAttribute("redirectAfterLogin",
                req.getContextPath() + req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo()));
        resp.sendRedirect(req.getContextPath() + "/login");
        return false;
    }
}
