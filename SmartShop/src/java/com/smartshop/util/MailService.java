package com.smartshop.util;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletContext;

import java.util.Properties;

public final class MailService {
    private MailService(){}

    public static void send(ServletContext ctx, String to, String subject, String content) {
        try {
            String host = ctx.getInitParameter("mail.smtp.host");
            String port = ctx.getInitParameter("mail.smtp.port");
            String user = ctx.getInitParameter("mail.smtp.username");
            String pass = ctx.getInitParameter("mail.smtp.password");
            boolean auth = "true".equalsIgnoreCase(ctx.getInitParameter("mail.smtp.auth"));
            boolean starttls = "true".equalsIgnoreCase(ctx.getInitParameter("mail.smtp.starttls.enable"));

            Properties p = new Properties();
            p.put("mail.smtp.host", host);
            if (port != null) p.put("mail.smtp.port", port);
            p.put("mail.smtp.auth", String.valueOf(auth));
            p.put("mail.smtp.starttls.enable", String.valueOf(starttls));

            Session session = Session.getInstance(p, auth ? new Authenticator(){
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            } : null);

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(user));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            msg.setSubject(subject);
            msg.setText(content);
            Transport.send(msg);
        } catch (Exception e){
            // log tối giản
            e.printStackTrace();
        }
    }
}
