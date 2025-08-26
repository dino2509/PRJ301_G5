package com.smartshop.servlet.admin;

import com.smartshop.dao.UserDAO;
import com.smartshop.model.User;
import com.smartshop.util.DB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.List;

@WebServlet(urlPatterns = {"/admin/users","/admin/users/*"})
public class UserAdminServlet extends HttpServlet {
    private static final String VIEW = "/WEB-INF/views/admin/users.jsp";
    private final UserDAO userDAO = new UserDAO();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String q = nn(req.getParameter("q"));
        List<User> list = userDAO.list(0, 1000, q.isEmpty()?null:q);
        req.setAttribute("list", list);
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = nn(req.getParameter("action")).toLowerCase();
        if (action.isEmpty()) action = req.getParameter("id")!=null? "update" : "create";

        try {
            switch (action) {
                case "create": {
                    String username = nn(req.getParameter("username"));
                    String email    = paramOr(req, new String[]{"email","mail"}, "");
                    String fullName = paramOr(req, new String[]{"full_name","name","fullname"}, "");
                    String phone    = paramOr(req, new String[]{"phone","phone_number","mobile"}, "");
                    String address  = paramOr(req, new String[]{"address","addr"}, "");
                    String password = paramOr(req, new String[]{"password","pass"}, "");
                    String role     = paramOr(req, new String[]{"role","role_name"}, "USER");
                    if (!username.isEmpty() && !password.isEmpty()) {
                        userDAO.create(username, email, fullName, phone, address, password, role);
                    }
                    break;
                }
                case "update": {
                    int id = ip(req.getParameter("id"), -1);
                    if (id > 0) {
                        User u = userDAO.findById(id);
                        if (u != null) {
                            // cập nhật qua DAO nếu có
                            try {
                                u.setEmail(paramOr(req, new String[]{"email","mail"}, u.getEmail()));
                                u.setFullName(paramOr(req, new String[]{"full_name","name","fullname"}, u.getFullName()));
                                u.setPhone(paramOr(req, new String[]{"phone","phone_number","mobile"}, u.getPhone()));
                                u.setAddress(paramOr(req, new String[]{"address","addr"}, u.getAddress()));
                                String role = paramOr(req, new String[]{"role","role_name"}, u.getRole());
                                if (!role.isEmpty()) u.setRole(role);
                                String status = paramOr(req, new String[]{"status","active","is_active"}, u.getStatus());
                                if (!status.isEmpty()) u.setStatus(status);

                                String newPass = nn(req.getParameter("password_new"));
                                if (newPass.isEmpty()) newPass = nn(req.getParameter("new_password"));
                                if (!newPass.isEmpty()) {
                                    try { User.class.getMethod("setPasswordHash", String.class).invoke(u, sha256(newPass)); }
                                    catch (Throwable t) { try { User.class.getMethod("setPassword", String.class).invoke(u, newPass); } catch (Throwable ignore) {} }
                                }
                                userDAO.update(u);
                            } catch (Throwable ignore) {}

                            // luôn chạy fallback JDBC để đảm bảo cập nhật
                            updateUserFallback(id, req);
                        }
                    }
                    break;
                }
                case "delete": {
                    int id = ip(req.getParameter("id"), -1);
                    if (id > 0) userDAO.delete(id);
                    break;
                }
            }
        } catch (Exception ignore) { }
        resp.sendRedirect(req.getContextPath() + "/admin/users");
    }

    // ---- JDBC fallback: update theo cột có thật ----
    private void updateUserFallback(int id, HttpServletRequest req) {
        try (Connection c = DB.getConnection()) {
            String uTable = tableExists(c,"Users")? "Users" : (tableExists(c,"User")? "User" : null);
            if (uTable==null) return;

            upd(c,uTable,id,"email",     paramOr(req,new String[]{"email","mail"}, null));
            upd(c,uTable,id,"full_name", paramOr(req,new String[]{"full_name","name","fullname"}, null));
            upd(c,uTable,id,"name",      paramOr(req,new String[]{"name","full_name"}, null));
            upd(c,uTable,id,"phone",     paramOr(req,new String[]{"phone","phone_number","mobile"}, null));
            upd(c,uTable,id,"address",   paramOr(req,new String[]{"address","addr"}, null));
            upd(c,uTable,id,"role",      paramOr(req,new String[]{"role","role_name"}, null));

            String status = paramOr(req,new String[]{"status","active","is_active"}, null);
            if (status != null) {
                if (hasColumn(c,uTable,"is_active")) updTyped(c,uTable,id,"is_active", toInt(status, null), Types.INTEGER);
                if (hasColumn(c,uTable,"status"))    updTyped(c,uTable,id,"status",    status,               Types.VARCHAR);
            }

            String newPass = nn(req.getParameter("password_new"));
            if (newPass.isEmpty()) newPass = nn(req.getParameter("new_password"));
            if (!newPass.isEmpty()) {
                if (hasColumn(c,uTable,"password_hash")) updTyped(c,uTable,id,"password_hash", sha256(newPass), Types.VARCHAR);
                if (hasColumn(c,uTable,"password"))      updTyped(c,uTable,id,"password",      newPass,          Types.VARCHAR);
            }
        } catch (Exception ignore) {}
    }

    private static void upd(Connection c, String table, int id, String col, String val) {
        if (val==null || !hasColumn(c,table,col)) return;
        String sql = "UPDATE "+table+" SET "+col+"=? WHERE id=?";
        try (PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setString(1, val); ps.setInt(2, id); ps.executeUpdate();
        } catch (SQLException ignore) {}
    }
    private static void updTyped(Connection c, String table, int id, String col, Object val, int sqlType) {
        if (val==null || !hasColumn(c,table,col)) return;
        String sql = "UPDATE "+table+" SET "+col+"=? WHERE id=?";
        try (PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setObject(1, val, sqlType); ps.setInt(2, id); ps.executeUpdate();
        } catch (SQLException ignore) {}
    }
    private static boolean hasColumn(Connection c, String table, String col){
        try(ResultSet rs=c.getMetaData().getColumns(null,null,table,col)){ if(rs.next()) return true; }
        catch(SQLException ignore){}
        try(ResultSet rs=c.getMetaData().getColumns(null,null,"dbo."+table,col)){ if(rs.next()) return true; }
        catch(SQLException ignore){}
        return false;
    }
    private static boolean tableExists(Connection c, String t){
        try(ResultSet rs=c.getMetaData().getTables(null,null,t,null)){ return rs.next(); }
        catch(SQLException e){ return false; }
    }

    // ===== utils =====
    private static String nn(String s){ return s==null? "": s.trim(); }
    private static int ip(String s,int d){ try{ return Integer.parseInt(s);}catch(Exception e){ return d; } }
    private static String def(String s){ return s==null? "": s; }
    private static Integer toInt(String s, Integer def){
        try { return (s==null||s.isBlank())? def : Integer.valueOf(s.trim()); }
        catch(Exception e){ return def; }
    }
    /** Lấy giá trị tham số đầu tiên khác null trong danh sách tên; nếu không có trả def */
    private static String paramOr(HttpServletRequest r, String[] names, String def){
        for (String n : names) {
            String v = r.getParameter(n);
            if (v != null) return v.trim();
        }
        return def;
    }
    private static String sha256(String s){
        try{ var md=MessageDigest.getInstance("SHA-256");
            var b=md.digest(s.getBytes(StandardCharsets.UTF_8));
            var sb=new StringBuilder(); for(byte x:b) sb.append(String.format("%02x",x)); return sb.toString();
        }catch(Exception e){ return s; }
    }
}
