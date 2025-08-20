package com.smartshop.servlet.admin;
import com.smartshop.dao.ProductDAO; import com.smartshop.dao.CategoryDAO; import com.smartshop.model.Product; import jakarta.servlet.*; import jakarta.servlet.http.*; import jakarta.servlet.annotation.*; import java.io.*;

@WebServlet(urlPatterns = "/admin/products")
public class ProductAdminServlet extends HttpServlet {
    private final ProductDAO productDAO=new ProductDAO(); private final CategoryDAO categoryDAO=new CategoryDAO();
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action==null) {
            req.setAttribute("list", productDAO.search(null,null,null,null,null,null,"newest",1,100));
            req.getRequestDispatcher("/WEB-INF/views/admin/products.jsp").forward(req, resp);
        } else if ("new".equals(action) || "edit".equals(action)) {
            Product p = "edit".equals(action)? productDAO.find(Integer.parseInt(req.getParameter("id"))) : new Product();
            req.setAttribute("p", p); req.setAttribute("categories", categoryDAO.findAll());
            req.getRequestDispatcher("/WEB-INF/views/admin/product_form.jsp").forward(req, resp);
        } else if ("delete".equals(action)) {
            productDAO.delete(Integer.parseInt(req.getParameter("id"))); resp.sendRedirect(req.getContextPath()+"/admin/products");
        } else { resp.sendError(404); }
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id=req.getParameter("id"); Product p = new Product();
        String cat=req.getParameter("category_id"); p.setCategoryId(cat==null||cat.isBlank()? null : Integer.valueOf(cat));
        p.setName(req.getParameter("name")); p.setBrand(req.getParameter("brand")); p.setColor(req.getParameter("color"));
        p.setDescription(req.getParameter("description")); p.setImageUrl(req.getParameter("image_url"));
        p.setPrice(new java.math.BigDecimal(req.getParameter("price"))); p.setStock(Integer.parseInt(req.getParameter("stock")));
        p.setActive("on".equals(req.getParameter("active")));
        if (id==null || id.isBlank()) productDAO.create(p); else { p.setId(Integer.parseInt(id)); productDAO.update(p);}
        resp.sendRedirect(req.getContextPath()+"/admin/products");
    }
}
