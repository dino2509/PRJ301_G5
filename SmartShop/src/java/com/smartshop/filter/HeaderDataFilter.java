package com.smartshop.filter;

import com.smartshop.util.DB;
import com.smartshop.dao.CategoryDAO;
import jakarta.servlet.*;
import java.io.IOException;

public class HeaderDataFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        // Nếu DB chưa sẵn sàng thì bỏ qua tải danh mục để tránh 500
        if (DB.isReady()) {
            try {
                req.setAttribute("headerCategories", new CategoryDAO().findAll());
            } catch (RuntimeException ignore) {
                // Không chặn request nếu lỗi DB cục bộ phần header
            }
        }
        chain.doFilter(req, res);
    }
}
