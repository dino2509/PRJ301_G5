package com.smartshop.servlet.shop;
import com.smartshop.dao.CartDAO; import com.smartshop.model.CartItem; import jakarta.servlet.*; import jakarta.servlet.http.*; import jakarta.servlet.annotation.*; import java.io.*; import java.util.*;

@WebServlet(urlPatterns = {"/cart/view","/cart/add","/cart/update","/cart/remove"})
public class CartServlet extends HttpServlet {
    private final CartDAO cartDAO = new CartDAO();
    private int uid(HttpServletRequest req){ return (Integer)req.getSession().getAttribute("uid"); }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/cart/view".equals(path)) {
            java.util.List<CartItem> items = cartDAO.listItems(uid(req));
            req.setAttribute("items", items);
            req.setAttribute("total", cartDAO.total(uid(req)));
            req.getRequestDispatcher("/WEB-INF/views/shop/cart.jsp").forward(req, resp);
        } else if ("/cart/add".equals(path)) {
            int pid = Integer.parseInt(req.getParameter("pid"));
            int qty = Integer.parseInt(req.getParameter("qty")==null? "1" : req.getParameter("qty"));
            cartDAO.addItem(uid(req), pid, qty);
            resp.sendRedirect(req.getContextPath()+"/cart/view");
        } else if ("/cart/remove".equals(path)) {
            int itemId = Integer.parseInt(req.getParameter("id")); cartDAO.removeItem(uid(req), itemId);
            resp.sendRedirect(req.getContextPath()+"/cart/view");
        } else { resp.sendError(404); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/cart/update".equals(req.getServletPath())) {
            int id=Integer.parseInt(req.getParameter("id")); int qty=Integer.parseInt(req.getParameter("qty"));
            cartDAO.updateQty(uid(req), id, qty);
            resp.sendRedirect(req.getContextPath()+"/cart/view");
        } else { resp.sendError(405); }
    }
}
