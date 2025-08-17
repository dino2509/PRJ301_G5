package com.smartshop.util;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")
public class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        res.setCharacterEncoding("UTF-8");
        if (res instanceof HttpServletResponse) {
            ((HttpServletResponse) res).setHeader("Content-Type", "text/html; charset=UTF-8");
        }
        chain.doFilter(req, res);
    }

    @Override public void init(FilterConfig filterConfig) {}
    @Override public void destroy() {}
}
