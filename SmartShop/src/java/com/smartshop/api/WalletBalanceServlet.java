package com.smartshop.api;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

// IMPORT DAO theo dự án của bạn
import com.smartshop.dao.WalletDAO;

public class WalletBalanceServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        Integer uid = null;
        try {
            uid = (Integer) req.getSession(false).getAttribute("uid"); // sửa "uid" nếu bạn dùng tên khác
        } catch (Exception ignored) {}

        if (uid == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\":\"unauthorized\"}");
            return;
        }

        try {
            WalletDAO dao = new WalletDAO();
            // Đổi tên method nếu dự án bạn khác (vd: getBalanceByUserId)
            BigDecimal bal = dao.getBalance(uid);
            if (bal == null) bal = BigDecimal.ZERO;

            out.write("{\"balance\":" + bal.toPlainString() + "}");
        } catch (Exception ex) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = ex.getMessage() == null ? "error" : ex.getMessage().replace("\"","'");
            out.write("{\"error\":\"" + msg + "\"}");
        }
    }
}
