package com.smartshop.servlet.auth;
import com.smartshop.dao.UserDAO; import com.smartshop.model.User;
import jakarta.servlet.*; import jakarta.servlet.http.*; import jakarta.servlet.annotation.*;
import java.io.*; import java.util.List;

@WebServlet(urlPatterns = "/login")
public class LoginServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username=req.getParameter("username"); String password=req.getParameter("password");
        User u = userDAO.findByUsername(username);
        if (u==null || !userDAO.verifyPassword(u, password)) { req.setAttribute("error","Invalid credentials"); doGet(req,resp); return; }

        HttpSession s = req.getSession(true);
        s.setAttribute("uid", u.getId());
        s.setAttribute("username", u.getUsername());
        List<String> roles = userDAO.rolesOf(u.getId());
        s.setAttribute("roles", roles);
        s.setAttribute("isAdmin", roles != null && roles.contains("ADMIN"));

        String pid=(String)s.getAttribute("pendingAddPid"); String qty=(String)s.getAttribute("pendingAddQty");
        s.removeAttribute("pendingAddPid"); s.removeAttribute("pendingAddQty");
        String redirect=(String)s.getAttribute("redirectAfterLogin"); s.removeAttribute("redirectAfterLogin");
        if (pid!=null) { resp.sendRedirect(req.getContextPath()+"/cart/add?pid="+pid+"&qty="+(qty==null?"1":qty)); return; }
        if (redirect!=null) { resp.sendRedirect(redirect); return; }
        resp.sendRedirect(req.getContextPath()+"/");
    }
}
