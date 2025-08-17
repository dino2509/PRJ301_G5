package com.smartshop.util;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class EmailUtil {

    /** Trả true nếu gửi OK; false nếu chưa cấu hình SMTP hoặc lỗi gửi. */
    public static boolean send(String to, String subject, String bodyText) {
        try {
            String host = getCfg("SMTP_HOST");
            String port = nvl(getCfg("SMTP_PORT"), "587");
            String user = getCfg("SMTP_USER");
            String pass = getCfg("SMTP_PASS");
            String from = nvl(getCfg("SMTP_FROM"), user);

            if (isBlank(host) || isBlank(user) || isBlank(pass)) {
                System.out.println("[EmailUtil] SMTP chưa cấu hình. Bỏ qua gửi email.");
                return false;
            }

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.mime.charset", "UTF-8"); // ép UTF-8 mọi phần

            Session session = Session.getInstance(props, new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            MimeMessage message = new MimeMessage(session);
            String fromEmail = nvl(getCfg("SMTP_FROM"), user);
            String fromName  = nvl(getCfg("SMTP_FROM_NAME"), "SmartShop");
            message.setFrom(new InternetAddress(fromEmail, fromName, java.nio.charset.StandardCharsets.UTF_8.name()));

            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, StandardCharsets.UTF_8.name());

            // Render HTML UTF-8, giữ xuống dòng và link click được
            String html = "<div style='font-family:Segoe UI,Roboto,Arial,sans-serif;font-size:14px'>"
                    + escape(bodyText).replace("\n", "<br>")
                    + "</div>";
            message.setContent(html, "text/html; charset=UTF-8");

            Transport.send(message);
            return true;
        } catch (Exception e) {
            System.out.println("[EmailUtil] Send failed: " + e.getMessage());
            return false;
        }
    }

    // ---------- helpers ----------
    private static volatile Properties cached;
    private static String getCfg(String key) {
        String v = System.getenv(key);
        if (!isBlank(v)) return v;
        v = System.getProperty(key);
        if (!isBlank(v)) return v;
        try {
            if (cached == null) {
                synchronized (EmailUtil.class) {
                    if (cached == null) {
                        Properties p = new Properties();
                        try (InputStream is = EmailUtil.class.getClassLoader().getResourceAsStream("smtp.properties")) {
                            if (is != null) p.load(is);
                        }
                        cached = p;
                    }
                }
            }
            v = cached.getProperty(key);
            if (!isBlank(v)) return v;
        } catch (Exception ignored) {}
        return null;
    }
    private static boolean isBlank(String s){ return s==null || s.trim().isEmpty(); }
    private static String nvl(String s, String d){ return isBlank(s)? d: s; }
    private static String escape(String s){
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
