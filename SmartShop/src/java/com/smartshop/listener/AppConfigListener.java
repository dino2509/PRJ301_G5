package com.smartshop.listener;

import com.smartshop.util.DB;
import jakarta.servlet.*;
import java.sql.*;

public class AppConfigListener implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        String url = ctx.getInitParameter("JDBC_URL");
        String user = ctx.getInitParameter("JDBC_USER");
        String pass = ctx.getInitParameter("JDBC_PASS");
        DB.configure(url, user, pass);
        try { Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver"); }
        catch (ClassNotFoundException e) { throw new RuntimeException(e); }
    }
}
