package com.smartshop.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import jakarta.servlet.annotation.WebFilter;

@WebFilter(urlPatterns = {"/cart/*", "/checkout", "/account/*", "/product/review"})

public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        Integer uid = (session == null) ? null : (Integer) session.getAttribute("uid");
        if (uid == null) {
            String target = req.getRequestURI() + (req.getQueryString() != null ? ("?" + req.getQueryString()) : "");
            req.getSession(true).setAttribute("redirectAfterLogin", target);
            String pid = req.getParameter("pid");
            String qty = req.getParameter("qty");
            if (pid != null) {
                req.getSession().setAttribute("pendingAddPid", pid);
                req.getSession().setAttribute("pendingAddQty", qty == null ? "1" : qty);
            }
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        chain.doFilter(request, response);
    }
}
