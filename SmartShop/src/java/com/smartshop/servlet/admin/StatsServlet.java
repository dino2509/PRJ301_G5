package com.smartshop.servlet.admin;

import com.smartshop.dao.StatsDAO;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/admin/stats")
public class StatsServlet extends HttpServlet {
    private final StatsDAO stats = new StatsDAO();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        var day   = stats.revenueByDay();
        var month = stats.revenueByMonth();
        var best  = stats.bestSellingProducts();
        var top   = stats.topRatedProducts();
        var pcat  = stats.productCountByCategory();
        var newc  = stats.newCustomersByDay();

        req.setAttribute("revDay", day);   req.setAttribute("revByDay", day);   req.setAttribute("revenueByDay", day);
        req.setAttribute("revMonth", month); req.setAttribute("revByMonth", month); req.setAttribute("revenueByMonth", month);
        req.setAttribute("best", best);    req.setAttribute("bestSelling", best);
        req.setAttribute("top", top);      req.setAttribute("topRated", top);
        req.setAttribute("pcat", pcat);    req.setAttribute("productsByCategory", pcat);
        req.setAttribute("newCus", newc);  req.setAttribute("newCustomers", newc);

        req.getRequestDispatcher("/WEB-INF/views/admin/stats.jsp").forward(req, resp);
    }
}
