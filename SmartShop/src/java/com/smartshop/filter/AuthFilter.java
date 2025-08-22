// src/main/java/com/smartshop/filter/AuthFilter.java
package com.smartshop.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebFilter(urlPatterns = {
        "/checkout",
        "/wallet",
        "/orders/*"
})
public class AuthFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req  = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        Object authUser = (session == null) ? null : session.getAttribute("authUser");
        Object uid = (session == null) ? null : session.getAttribute("uid");

        if (authUser == null && uid == null) {
            String original = req.getRequestURI();
            String qs = req.getQueryString();
            if (qs != null && !qs.isEmpty()) original += "?" + qs;
            String next = URLEncoder.encode(original, StandardCharsets.UTF_8);
            resp.sendRedirect(req.getContextPath() + "/login?next=" + next);
            return;
        }

        chain.doFilter(request, response);
    }
}
