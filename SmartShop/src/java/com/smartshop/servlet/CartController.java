package com.smartshop.servlet;
import com.smartshop.dao.ProductDAO;
import com.smartshop.model.CartItem;
import com.smartshop.model.Product;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
@WebServlet(urlPatterns={"/cart"})
public class CartController extends HttpServlet {
    private final ProductDAO productDAO=new ProductDAO();
    @SuppressWarnings("unchecked")
    private java.util.Map<Integer, CartItem> getCart(HttpSession session){
        Object obj=session.getAttribute("cart");
        if(obj==null){ java.util.Map<Integer,CartItem> m=new java.util.LinkedHashMap<>(); session.setAttribute("cart", m); return m; }
        return (java.util.Map<Integer,CartItem>)obj;
    }
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/cart.jsp").forward(req, resp);
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action=req.getParameter("action"); int id=Integer.parseInt(req.getParameter("id"));
        java.util.Map<Integer, CartItem> cart=getCart(req.getSession());
        switch(action){
            case "add" -> {
                Product p=productDAO.findById(id);
                if(p!=null){
                    CartItem it=cart.getOrDefault(id, new CartItem(p,0));
                    it.setQuantity(it.getQuantity()+1); cart.put(id,it);
                }
            }
            case "update" -> {
                int qty=Integer.parseInt(req.getParameter("qty"));
                CartItem it=cart.get(id); if(it!=null){ it.setQuantity(qty); if(qty<=0) cart.remove(id);}
            }
            case "remove" -> cart.remove(id);
        }
        resp.sendRedirect("cart");
    }
}
