package com.smartshop.servlet.auth;

import com.smartshop.dao.UserDAO;
import com.smartshop.model.User;
import com.smartshop.util.EmailUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@WebServlet(urlPatterns = "/forgot")
public class ForgotServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();
    private static final SecureRandom RNG = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("send".equalsIgnoreCase(action)) {
            handleSend(req, resp);
        } else if ("verify".equalsIgnoreCase(action)) {
            handleVerify(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/forgot");
        }
    }

    private void handleSend(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String email = req.getParameter("email");
        if (email != null) email = email.trim();

        // luôn giữ lại email trên form
        req.setAttribute("email", email);

        // tạo mã 6 số
        String code = String.format("%06d", RNG.nextInt(1_000_000));

        User u = (email == null || email.isBlank()) ? null : userDAO.findByEmail(email);
        if (u != null) {
            // lưu code=token và hạn 5 phút
            userDAO.setResetCodeAndToken(u.getId(), code, 5);

            String base = req.getScheme() + "://" + req.getServerName() +
                    (isDefaultPort(req) ? "" : (":" + req.getServerPort())) + req.getContextPath();

            String link = base + "/reset?token=" + url(code) + "&email=" + url(u.getEmail());

            String html = "<p>Verification code:</p>" +
                    "<h2 style='letter-spacing:2px'>" + code + "</h2>" +
                    "<p>Or reset directly:</p>" +
                    "<p><a href='" + link + "'>" + link + "</a></p>" +
                    "<p>Expires in 5 minutes or after use.</p>";

            EmailUtil.send(u.getEmail(), "SmartShop: Reset password", html);
        }

        req.setAttribute("message", "Nếu email tồn tại, mã xác nhận và liên kết đặt lại đã được gửi.");
        req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
    }

    private void handleVerify(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String code = req.getParameter("code");
        if (code != null) code = code.trim();

        User u = (code == null || code.isBlank()) ? null : userDAO.findByResetCode(code);

        if (u == null) {
            req.setAttribute("error", "Mã xác nhận không đúng hoặc đã hết hạn.");
            // giữ lại email nếu người dùng đã nhập ở bước gửi mã
            req.setAttribute("email", req.getParameter("email"));
            req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
            return;
        }

        // đúng mã -> chuyển thẳng tới /reset kèm cả token và email
        resp.sendRedirect(req.getContextPath() + "/reset?token=" + url(code) + "&email=" + url(u.getEmail()));
    }

    private boolean isDefaultPort(HttpServletRequest req) {
        int port = req.getServerPort();
        String scheme = req.getScheme();
        return ("http".equalsIgnoreCase(scheme) && port == 80) ||
               ("https".equalsIgnoreCase(scheme) && port == 443);
    }

    private String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
