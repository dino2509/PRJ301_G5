package com.smartshop.servlet.admin;
import com.smartshop.dao.StatsDAO; import jakarta.servlet.*; import jakarta.servlet.http.*; import jakarta.servlet.annotation.*; import java.io.*;

@WebServlet(urlPatterns = "/admin/stats")
public class StatsServlet extends HttpServlet {
    private final StatsDAO stats=new StatsDAO();
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("revDay", stats.revenueByDay());
        req.setAttribute("revMonth", stats.revenueByMonth());
        req.setAttribute("best", stats.bestSellingProducts());
        req.setAttribute("top", stats.topRatedProducts());
        req.setAttribute("pcat", stats.productCountByCategory());
        req.setAttribute("newCus", stats.newCustomersByDay());
        req.getRequestDispatcher("/WEB-INF/views/admin/stats.jsp").forward(req, resp);
    }
}
