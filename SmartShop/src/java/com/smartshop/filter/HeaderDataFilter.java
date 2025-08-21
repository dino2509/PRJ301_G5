package com.smartshop.filter;

import com.smartshop.util.DB;
import com.smartshop.dao.CategoryDAO;
import com.smartshop.dao.CartDAO;
import com.smartshop.model.CartItem;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

public class HeaderDataFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // Header categories (giữ nguyên nếu bạn đã có)
        if (DB.isReady()) {
            try { req.setAttribute("headerCategories", new CategoryDAO().findAll()); }
            catch (RuntimeException ignore) {}
        }

        // Đồng bộ giỏ: khi có uid
        HttpServletRequest http = (HttpServletRequest) req;
        HttpSession session = http.getSession(false);
        if (session != null) {
            Integer uid = (Integer) session.getAttribute("uid");
            if (uid != null && DB.isReady()) {
                // Merge 1 lần các item guest -> DB
                if (session.getAttribute("cartMerged") == null) {
                    @SuppressWarnings("unchecked")
                    Map<Integer, CartItem> cart = (Map<Integer, CartItem>) session.getAttribute("cart");
                    CartDAO.safeMergeSessionToUserCart(uid, cart);
                    session.setAttribute("cartMerged", Boolean.TRUE);
                }
                // Nếu session chưa có giỏ, tải từ DB
                if (session.getAttribute("cart") == null) {
                    CartDAO.safeLoadCartToSession(uid, session);
                }
            }
        }

        chain.doFilter(req, res);
    }
}
