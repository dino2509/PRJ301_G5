package com.smartshop.servlet;

import com.smartshop.dao.UserDAO;
import com.smartshop.model.User;
import com.smartshop.util.EmailUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.servlet.annotation.WebServlet;

@WebServlet(urlPatterns = {"/login", "/logout", "/register", "/forgot", "/reset", "/account", "/account/password"})

public class AuthController extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String p = req.getServletPath();
        switch (p) {
            case "/login" ->
                showLogin(req, resp);
            case "/logout" ->
                doLogout(req, resp);
            case "/register" ->
                req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
            case "/forgot" ->
                req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
            case "/reset" ->
                showReset(req, resp);                     // ĐÃ CHẶN BƯỚC NÀY
            case "/account" ->
                showAccount(req, resp);
            case "/account/password" ->
                requireLoginThen(req, resp, "/WEB-INF/views/account/password.jsp");
            default ->
                resp.sendError(404);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String p = req.getServletPath();
        switch (p) {
            case "/login" ->
                doLogin(req, resp);
            case "/register" ->
                doRegister(req, resp);
            case "/forgot" ->
                doForgot(req, resp);                     // GỬI MÃ hoặc XÁC MINH MÃ
            case "/reset" ->
                doReset(req, resp);                       // ĐỔI MẬT KHẨU (yêu cầu token+email)
            case "/account" ->
                doUpdateProfile(req, resp);
            case "/account/password" ->
                doChangePassword(req, resp);
            default ->
                resp.sendError(404);
        }
    }

    /* ================== Login ================== */
    private void showLogin(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    private void doLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        User u = userDAO.findByUsername(username);
        if (u == null || !userDAO.verifyPassword(u, password)) {
            req.setAttribute("error", "Invalid credentials");
            showLogin(req, resp);
            return;
        }
        if ("LOCKED".equalsIgnoreCase(u.getStatus())) {
            req.setAttribute("error", "Account locked");
            showLogin(req, resp);
            return;
        }
        HttpSession s = req.getSession(true);
        s.setAttribute("uid", u.getId());
        s.setAttribute("username", u.getUsername());
        List<String> roles = userDAO.rolesOf(u.getId());
        s.setAttribute("roles", roles);
        s.setAttribute("isAdmin", roles != null && roles.contains("ADMIN"));

        String pid = (String) s.getAttribute("pendingAddPid");
        String qty = (String) s.getAttribute("pendingAddQty");
        s.removeAttribute("pendingAddPid");
        s.removeAttribute("pendingAddQty");
        String redirect = (String) s.getAttribute("redirectAfterLogin");
        s.removeAttribute("redirectAfterLogin");
        if (pid != null) {
            resp.sendRedirect(req.getContextPath() + "/cart/add?pid=" + pid + "&qty=" + (qty == null ? "1" : qty));
            return;
        }
        if (redirect != null) {
            resp.sendRedirect(redirect);
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/home");
    }

    private void doLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s != null) {
            s.invalidate();
        }
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    /* ================== Register ================== */
    private void doRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String email = req.getParameter("email");
        String full = req.getParameter("full_name");
        String phone = req.getParameter("phone");
        String address = req.getParameter("address");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            req.setAttribute("error", "Thiếu username hoặc password");
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
            return;
        }
        if (userDAO.usernameExists(username)) {
            req.setAttribute("error", "Username đã tồn tại");
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
            return;
        }
        String err = userDAO.createWithError(buildUser(username, email, phone, full,address), password);
        if (err != null) {
            req.setAttribute("error", err);
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/login");
        }
    }

    /* ================== Account ================== */
    private void showAccount(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer uid = (Integer) req.getSession().getAttribute("uid");
        if (uid == null) {
            req.getSession().setAttribute("redirectAfterLogin", req.getRequestURI());
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        User u = userDAO.findById(uid);
        if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        req.setAttribute("user", u);
        req.getRequestDispatcher("/WEB-INF/views/account/profile.jsp").forward(req, resp);
    }

    private void doUpdateProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Integer uid = (Integer) req.getSession().getAttribute("uid");
        if (uid == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        User auth = userDAO.findById(uid);
        if (auth == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        auth.setEmail(req.getParameter("email"));
        auth.setPhone(req.getParameter("phone"));
        auth.setFullName(req.getParameter("full_name"));
        auth.setAddress(req.getParameter("address"));
        boolean ok = userDAO.updateProfile(auth);
        req.setAttribute("msg", ok ? "Cập nhật thành công" : "Cập nhật thất bại");
        req.setAttribute("user", auth);
        req.getRequestDispatcher("/WEB-INF/views/account/profile.jsp").forward(req, resp);
    }

    private void doChangePassword(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Integer uid = (Integer) req.getSession().getAttribute("uid");
        if (uid == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        User auth = userDAO.findById(uid);
        if (auth == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String oldPass = req.getParameter("old_password");
        String newPass = req.getParameter("new_password");
        if (oldPass == null || !userDAO.verifyPassword(auth, oldPass)) {
            req.setAttribute("error", "Mật khẩu cũ không đúng");
        } else if (userDAO.changePassword(auth.getId(), newPass)) {
            req.setAttribute("msg", "Đổi mật khẩu thành công");
        } else {
            req.setAttribute("error", "Đổi mật khẩu thất bại");
        }
        req.getRequestDispatcher("/WEB-INF/views/account/password.jsp").forward(req, resp);
    }

    /* ================== Forgot / Reset ================== */
    // /forgot: gửi mã HOẶC xác minh mã. Nếu mã đúng -> chuyển sang /reset?token=CODE&email=EMAIL
    private void doForgot(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String action = trim(req.getParameter("action"));
        if ("verify".equalsIgnoreCase(action)) {
            String code = trim(req.getParameter("code"));
            User u = (code != null && code.matches("\\d{6}")) ? userDAO.findByResetCode(code) : null;
            if (u == null) {
                req.setAttribute("error", "Mã không hợp lệ hoặc đã hết hạn");
                // giữ lại email nếu người dùng đã nhập trước đó
                req.setAttribute("email", trim(req.getParameter("email")));
                req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
                return;
            }
            // đúng mã -> sang /reset kèm token (code) + email
            resp.sendRedirect(req.getContextPath() + "/reset?token=" + url(code) + "&email=" + url(u.getEmail()));
            return;
        }

        // mặc định: gửi mã
        String email = trim(req.getParameter("email"));
        req.setAttribute("email", email); // giữ giá trị trên form

        User u = (email == null) ? null : userDAO.findByEmail(email);
        if (u != null) {
            // token = code; hạn 5 phút
            String code = String.format("%06d", (int) (Math.random() * 1_000_000));
            LocalDateTime exp = LocalDateTime.now().plusMinutes(5);
            userDAO.saveResetChallenge(u.getId(), code, code, exp);

            String base = req.getScheme() + "://" + req.getServerName()
                    + (isDefaultPort(req) ? "" : (":" + req.getServerPort())) + req.getContextPath();
            String link = base + "/reset?token=" + url(code) + "&email=" + url(u.getEmail());

            String html = "<p>Mã xác nhận: <b>" + code + "</b></p>"
                    + "<p>Hoặc đặt lại trực tiếp: <a href=\"" + link + "\">" + link + "</a></p>"
                    + "<p>Hết hạn sau 5 phút hoặc sau khi sử dụng.</p>";
            EmailUtil.send(u.getEmail(), "SmartShop - Reset password", html);
        }

        req.setAttribute("info", "Nếu email tồn tại, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu.");
        req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
    }

    // GET /reset: bắt buộc đủ token + email. Hợp lệ -> hiển thị form với email khóa.
    private void showReset(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = trim(req.getParameter("token"));
        String email = trim(req.getParameter("email"));

        if (isBlank(token) || isBlank(email)) {
            resp.sendRedirect(req.getContextPath() + "/forgot");
            return;
        }

        User u = userDAO.findByResetToken(token);          // DAO phải kiểm tra hạn dùng
        if (u == null || u.getEmail() == null || !u.getEmail().equalsIgnoreCase(email)) {
            req.getRequestDispatcher("/WEB-INF/views/auth/reset_expired.jsp").forward(req, resp);
            return;
        }

        req.setAttribute("token", token);
        req.setAttribute("resetEmail", u.getEmail());       // hiển thị trên form
        req.getRequestDispatcher("/WEB-INF/views/auth/reset.jsp").forward(req, resp);
    }

    // POST /reset: đổi mật khẩu nếu token + email hợp lệ
    private void doReset(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String token = trim(req.getParameter("token"));
        String email = trim(req.getParameter("email"));
        String step = trim(req.getParameter("step"));

        // Nếu có nhánh cũ dùng step=verify, vẫn hỗ trợ để không phá form hiện tại
        if ("verify".equalsIgnoreCase(step)) {
            String code = trim(req.getParameter("code"));
            User u = (code != null && code.matches("\\d{6}")) ? userDAO.findByResetCode(code) : null;
            if (u == null) {
                req.setAttribute("error", "Mã không hợp lệ hoặc đã hết hạn");
                req.getRequestDispatcher("/WEB-INF/views/auth/reset.jsp").forward(req, resp);
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/reset?token=" + url(code) + "&email=" + url(u.getEmail()));
            return;
        }

        if (isBlank(token) || isBlank(email)) {
            resp.sendRedirect(req.getContextPath() + "/forgot");
            return;
        }

        User u = userDAO.findByResetToken(token);
        if (u == null || u.getEmail() == null || !u.getEmail().equalsIgnoreCase(email)) {
            req.getRequestDispatcher("/WEB-INF/views/auth/reset_expired.jsp").forward(req, resp);
            return;
        }

        String newPass = req.getParameter("new_password");
        String confirm = req.getParameter("confirm_password");
        if (isBlank(newPass) || !newPass.equals(confirm)) {
            req.setAttribute("token", token);
            req.setAttribute("resetEmail", u.getEmail());
            req.setAttribute("error", "Mật khẩu không hợp lệ hoặc không khớp");
            req.getRequestDispatcher("/WEB-INF/views/auth/reset.jsp").forward(req, resp);
            return;
        }

        if (userDAO.changePassword(u.getId(), newPass)) {
            userDAO.clearResetToken(u.getId()); // vô hiệu hóa link cũ
            req.setAttribute("msg", "Đổi mật khẩu thành công");
            resp.sendRedirect(req.getContextPath() + "/login?msg=reset_ok");
        } else {
            req.setAttribute("token", token);
            req.setAttribute("resetEmail", u.getEmail());
            req.setAttribute("error", "Đổi mật khẩu thất bại");
            req.getRequestDispatcher("/WEB-INF/views/auth/reset.jsp").forward(req, resp);
        }
    }

    /* ================== Helpers ================== */
    private void requireLoginThen(HttpServletRequest req, HttpServletResponse resp, String view) throws IOException, ServletException {
        if (req.getSession().getAttribute("uid") == null) {
            req.getSession().setAttribute("redirectAfterLogin", req.getRequestURI());
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        req.getRequestDispatcher(view).forward(req, resp);
    }

    private static User buildUser(String username, String email, String phone, String full, String address) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPhone(phone);
        u.setFullName(full);
        u.setAddress(address);
        return u;
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isDefaultPort(HttpServletRequest req) {
        int p = req.getServerPort();
        String sch = req.getScheme();
        return ("http".equalsIgnoreCase(sch) && p == 80) || ("https".equalsIgnoreCase(sch) && p == 443);
    }

    private String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
