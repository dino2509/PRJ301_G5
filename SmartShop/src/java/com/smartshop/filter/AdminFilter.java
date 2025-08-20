package com.smartshop.filter;
import jakarta.servlet.*; import jakarta.servlet.http.*; import java.io.IOException; import java.util.*;

public class AdminFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req=(HttpServletRequest)request; HttpServletResponse resp=(HttpServletResponse)response; HttpSession s=req.getSession(false);
        if (s==null || s.getAttribute("roles")==null || !((java.util.List<String>)s.getAttribute("roles")).contains("ADMIN")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
