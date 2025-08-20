package com.smartshop.servlet.admin;
import com.smartshop.dao.UserDAO; import com.smartshop.model.User; import jakarta.servlet.*; import jakarta.servlet.http.*; import jakarta.servlet.annotation.*; import java.io.*;

@WebServlet(urlPatterns = "/admin/users")
public class UserAdminServlet extends HttpServlet {
    private final UserDAO userDAO=new UserDAO();
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("list", userDAO.list(0,200, req.getParameter("q")));
        req.getRequestDispatcher("/WEB-INF/views/admin/users.jsp").forward(req, resp);
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String act=req.getParameter("action");
        if("create".equals(act))
            userDAO.create(req.getParameter("username"), req.getParameter("email"), req.getParameter("full_name"),
                           req.getParameter("phone"), req.getParameter("password"), req.getParameter("role"));
        else if("delete".equals(act))
            userDAO.delete(Integer.parseInt(req.getParameter("id")));
        else if("update".equals(act)) {
            User u=new User();
            u.setId(Integer.parseInt(req.getParameter("id")));
            u.setEmail(req.getParameter("email"));
            u.setFullName(req.getParameter("full_name"));
            u.setPhone(req.getParameter("phone"));
            u.setStatus(req.getParameter("status"));
            userDAO.update(u);
        }
        resp.sendRedirect(req.getContextPath()+"/admin/users");
    }
}
