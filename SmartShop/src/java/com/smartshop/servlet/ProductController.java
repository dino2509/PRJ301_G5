package com.smartshop.servlet;
import com.smartshop.dao.ProductDAO;
import com.smartshop.model.Product;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebServlet(urlPatterns={"/product"})
public class ProductController extends HttpServlet {
    private final ProductDAO productDAO=new ProductDAO();
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int id=Integer.parseInt(req.getParameter("id"));
        Product p=productDAO.findById(id);
        if(p==null){ resp.sendError(404); return; }
        req.setAttribute("p", p);
        req.setAttribute("related", productDAO.top("featured", 4));
        req.getRequestDispatcher("/WEB-INF/views/product_detail.jsp").forward(req, resp);
    }
}
