package com.smartshop.filter;
import com.smartshop.dao.CategoryDAO; import jakarta.servlet.*; import java.io.IOException;
public class HeaderDataFilter implements Filter {
    private final CategoryDAO categoryDAO=new CategoryDAO();
    @Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        req.setAttribute("categories", categoryDAO.findAll());
        chain.doFilter(req,res);
    }
}
