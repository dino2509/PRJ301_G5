package com.smartshop.servlet.shop;
import com.smartshop.dao.ReviewDAO; import jakarta.servlet.*; import jakarta.servlet.http.*; import jakarta.servlet.annotation.*; import java.io.*;

@WebServlet(urlPatterns = "/product/review")
public class ReviewServlet extends HttpServlet {
    private final ReviewDAO dao=new ReviewDAO();
    private int uid(HttpServletRequest req){ return (Integer)req.getSession().getAttribute("uid"); }
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("pid", req.getParameter("productId"));
        req.getRequestDispatcher("/WEB-INF/views/shop/review.jsp").forward(req, resp);
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int pid=Integer.parseInt(req.getParameter("productId")); int rating=Integer.parseInt(req.getParameter("rating"));
        String comment=req.getParameter("comment");
        dao.upsert(uid(req), pid, rating, comment);
        resp.sendRedirect(req.getContextPath()+"/product?id="+pid);
    }
}
