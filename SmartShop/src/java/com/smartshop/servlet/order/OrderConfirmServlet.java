// src/main/java/com/smartshop/servlet/order/OrderConfirmServlet.java
package com.smartshop.servlet.order;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet(urlPatterns = {"/orders/confirm"})
public class OrderConfirmServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("orderId", req.getParameter("id"));
        RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/views/order/confirm.jsp");
        rd.forward(req, resp);
    }
}
