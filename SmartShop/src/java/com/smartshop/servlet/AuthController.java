package com.smartshop.servlet;

import com.smartshop.dao.UserDAO;
import com.smartshop.model.User;
import com.smartshop.util.EmailUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import org.mindrot.jbcrypt.BCrypt;

@WebServlet(urlPatterns = {
        "/login", "/register", "/logout",
        "/profile", "/profile/update", "/change-password",
        "/forgot", "/reset"
})
public class AuthController extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (req.getServletPath()) {
            case "/login" -> req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            case "/register" -> req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            case "/forgot" -> req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
            case "/profile" -> {
                if (req.getSession().getAttribute("auth") == null) { resp.sendRedirect("login"); return; }
                req.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(req, resp);
            }
            case "/reset" -> { // không hỏi mã; hợp lệ nếu có token hợp lệ hoặc đã verify code trước đó
                HttpSession ses = req.getSession();
                Integer uid = (Integer) ses.getAttribute("resetUserId");
                String token = req.getParameter("token");
                if (uid == null && token != null && !token.isBlank()) {
                    User u = userDAO.findByResetToken(token);
                    if (u != null) { ses.setAttribute("resetUserId", u.getId()); uid = u.getId(); }
                }
                if (uid == null) {
                    req.setAttribute("msg", "Mã/liên kết không hợp lệ hoặc đã hết hạn");
                    req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
                    return;
                }
                req.getRequestDispatcher("/WEB-INF/views/reset.jsp").forward(req, resp);
            }
            case "/logout" -> { req.getSession().invalidate(); resp.sendRedirect("login"); }
            default -> resp.sendError(404);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        switch (req.getServletPath()) {
            case "/login" -> {
                User user = userDAO.authenticate(req.getParameter("username"), req.getParameter("password"));
                if (user != null) { req.getSession().setAttribute("auth", userDAO.findByUsername(user.getUsername())); resp.sendRedirect("home"); }
                else { req.setAttribute("error", "Sai tài khoản hoặc mật khẩu"); req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp); }
            }
            case "/register" -> {
                String username = req.getParameter("username");
                String password = req.getParameter("password");
                String confirm  = req.getParameter("confirm");
                String email    = req.getParameter("email");
                String phone    = req.getParameter("phone");
                String full     = req.getParameter("fullName");

                if (userDAO.usernameExists(username)) {
                    req.setAttribute("error","Username đã tồn tại"); req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp); return;
                }
                if (password == null || password.length() < 6) {
                    req.setAttribute("error","Mật khẩu tối thiểu 6 ký tự"); req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp); return;
                }
                if (!password.equals(confirm)) {
                    req.setAttribute("error","Xác nhận mật khẩu không khớp"); req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp); return;
                }

                User u = new User(); u.setUsername(username); u.setEmail(email); u.setPhone(phone); u.setFullName(full); u.setRole("CUSTOMER");
                String err = userDAO.createWithError(u, password);
                if (err == null) resp.sendRedirect("login");
                else { req.setAttribute("error","Đăng ký thất bại: "+err); req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp); }
            }
            case "/profile/update" -> {
                User auth = (User) req.getSession().getAttribute("auth");
                if (auth == null) { resp.sendRedirect("login"); return; }
                auth.setFullName(req.getParameter("fullName"));
                auth.setEmail(req.getParameter("email"));
                auth.setPhone(req.getParameter("phone"));
                if (userDAO.updateProfile(auth)) {
                    req.getSession().setAttribute("auth", userDAO.findByUsername(auth.getUsername()));
                    req.setAttribute("msg","Cập nhật hồ sơ thành công");
                } else req.setAttribute("msg","Cập nhật thất bại");
                req.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(req, resp);
            }
            case "/change-password" -> {
                User auth = (User) req.getSession().getAttribute("auth");
                if (auth == null) { resp.sendRedirect("login"); return; }
                String oldPass = req.getParameter("oldPassword");
                String newPass = req.getParameter("newPassword");
                String confirm = req.getParameter("confirm");

                if (oldPass == null || !BCrypt.checkpw(oldPass, auth.getPasswordHash())) {
                    req.setAttribute("msg","Mật khẩu cũ không đúng");
                } else if (newPass == null || newPass.length() < 6) {
                    req.setAttribute("msg","Mật khẩu mới tối thiểu 6 ký tự");
                } else if (!newPass.equals(confirm)) {
                    req.setAttribute("msg","Xác nhận mật khẩu không khớp");
                } else if (userDAO.changePassword(auth.getId(), newPass)) {
                    req.getSession().setAttribute("auth", userDAO.findByUsername(auth.getUsername()));
                    req.setAttribute("msg","Đổi mật khẩu thành công");
                } else {
                    req.setAttribute("msg","Đổi mật khẩu thất bại");
                }
                req.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(req, resp);
            }
            case "/forgot" -> {
                String action = req.getParameter("action");
                if ("request".equals(action)) {
                    String email = req.getParameter("email");
                    User u = userDAO.findByEmail(email);
                    if (u != null) {
                        String token = UUID.randomUUID().toString();
                        String code  = String.format("%06d", new SecureRandom().nextInt(1_000_000));
                        LocalDateTime exp = LocalDateTime.now().plusMinutes(30);
                        userDAO.saveResetChallenge(u.getId(), token, code, exp);

                        String link = req.getRequestURL().toString().replace("/forgot","/reset") + "?token=" + token;
                        String html = """
                            <p>Xin chào %s,</p>
                            <p>Bạn yêu cầu đặt lại mật khẩu SmartShop.</p>
                            <p><b>MÃ XÁC NHẬN: %s</b> (hiệu lực 30 phút)</p>
                            <p>Hoặc bấm liên kết: <a href="%s">Đặt lại mật khẩu</a></p>
                            <p>Nếu không phải bạn, hãy bỏ qua email này.</p>
                        """.formatted(nvl(u.getFullName(), u.getUsername()), code, link);

                        boolean sent = EmailUtil.send(email, "Đặt lại mật khẩu SmartShop", html);
                        if (!sent) req.setAttribute("devResetLink", link);
                    }
                    req.setAttribute("msg","Nếu email tồn tại, mã và liên kết đặt lại đã được gửi.");
                    req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
                } else if ("verify".equals(action)) {
                    String code = req.getParameter("code");
                    User u = (code!=null && code.matches("\\d{6}")) ? userDAO.findByResetCode(code) : null;
                    if (u != null) {
                        req.getSession().setAttribute("resetUserId", u.getId());
                        resp.sendRedirect("reset");
                    } else {
                        req.setAttribute("msg","Mã xác nhận không hợp lệ hoặc đã hết hạn");
                        req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
                    }
                } else resp.sendError(400);
            }
            case "/reset" -> {
                HttpSession ses = req.getSession();
                Integer uid = (Integer) ses.getAttribute("resetUserId");
                User u = (uid != null) ? userDAO.findById(uid) : userDAO.findByResetToken(req.getParameter("token"));

                String newPass = req.getParameter("newPassword");
                String confirm = req.getParameter("confirm");

                if (u == null) {
                    req.setAttribute("msg","Phiên đặt lại không hợp lệ hoặc đã hết hạn");
                    req.getRequestDispatcher("/WEB-INF/views/forgot.jsp").forward(req, resp);
                    return;
                }
                if (newPass == null || newPass.length() < 6) {
                    req.setAttribute("msg","Mật khẩu tối thiểu 6 ký tự");
                    req.getRequestDispatcher("/WEB-INF/views/reset.jsp").forward(req, resp);
                    return;
                }
                if (!newPass.equals(confirm)) {
                    req.setAttribute("msg","Xác nhận mật khẩu không khớp");
                    req.getRequestDispatcher("/WEB-INF/views/reset.jsp").forward(req, resp);
                    return;
                }
                if (userDAO.changePassword(u.getId(), newPass)) {
                    userDAO.clearResetToken(u.getId());
                    ses.removeAttribute("resetUserId");
                    resp.sendRedirect("login");
                } else {
                    req.setAttribute("msg","Đổi mật khẩu thất bại");
                    req.getRequestDispatcher("/WEB-INF/views/reset.jsp").forward(req, resp);
                }
            }
        }
    }

    private static String nvl(String s, String d){ return (s==null || s.isBlank()) ? d : s; }
}
