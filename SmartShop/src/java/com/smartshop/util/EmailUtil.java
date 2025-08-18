package com.smartshop.util;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class EmailUtil {

    /** body là HTML. Trả true nếu gửi OK; false nếu chưa cấu hình SMTP hoặc lỗi. */
    public static boolean send(String to, String subject, String htmlBody) {
        try {
            String host = getCfg("SMTP_HOST");
            String port = nvl(getCfg("SMTP_PORT"), "587");
            String user = getCfg("SMTP_USER");
            String pass = getCfg("SMTP_PASS");
            String from = nvl(getCfg("SMTP_FROM"), user);
            String fromName = nvl(getCfg("SMTP_FROM_NAME"), "SmartShop");

            if (isBlank(host) || isBlank(user) || isBlank(pass)) {
                System.out.println("[EmailUtil] SMTP chưa cấu hình.");
                return false;
            }

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, StandardCharsets.UTF_8.name());
            message.setContent(htmlBody, "text/html; charset=UTF-8");

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
}
