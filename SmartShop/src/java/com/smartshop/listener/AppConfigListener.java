package com.smartshop.listener;

import com.smartshop.util.DB;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppConfigListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Khởi tạo cấu hình DB từ <context-param> trong web.xml
        DB.initFromContext(sce.getServletContext());
    }
}
