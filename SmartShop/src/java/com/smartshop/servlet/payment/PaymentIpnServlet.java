package com.smartshop.servlet.payment;

import com.smartshop.dao.PaymentDAO;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;

public class PaymentIpnServlet extends HttpServlet {
    private static final String SECRET = "demo-secret-123";
    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String provider = req.getParameter("provider");
        int orderId = Integer.parseInt(req.getParameter("orderId"));
        String txnId = req.getParameter("txnId");
        String amountStr = req.getParameter("amount");
        String sig = req.getParameter("sig");
        String calc = Integer.toHexString((txnId + amountStr + SECRET).hashCode());
        if (!calc.equals(sig)) {
            resp.sendError(400, "Invalid signature");
            return;
        }
        BigDecimal amount = new BigDecimal(amountStr);
        paymentDAO.recordPaymentAndMarkPaid(orderId, provider, txnId, amount, "DEMO");
        req.getSession().setAttribute("flash", "Payment success via " + provider + " for order #" + orderId);
        resp.sendRedirect(req.getContextPath() + "/orders/success.jsp?orderId=" + orderId);
    }
}
