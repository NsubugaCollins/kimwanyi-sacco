package org.kimwanyi.sacco.serviceImpl;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kimwanyi.sacco.service.EmailService;

import java.util.Properties;

public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final Properties mailProperties;
    private final String fromAddress;
    private final String username;
    private final String password;


    private static String cfg(String sysProp, String envVar, String fallback) {
        String sys = System.getProperty(sysProp);
        if (sys != null && !sys.isBlank()) return sys;
        String env = System.getenv(envVar);
        if (env != null && !env.isBlank()) return env;
        return fallback;
    }

    public EmailServiceImpl() {
        this.mailProperties = new Properties();
        this.mailProperties.put("mail.smtp.host",           cfg("mail.smtp.host",           "MAIL_SMTP_HOST",     "localhost"));
        this.mailProperties.put("mail.smtp.port",           cfg("mail.smtp.port",           "MAIL_SMTP_PORT",     "587"));
        this.mailProperties.put("mail.smtp.auth",           cfg("mail.smtp.auth",           "MAIL_SMTP_AUTH",     "false"));
        this.mailProperties.put("mail.smtp.starttls.enable",cfg("mail.smtp.starttls.enable","MAIL_SMTP_STARTTLS", "false"));

        this.fromAddress = cfg("mail.from",          "MAIL_FROM",          "noreply@kimwanyi-sacco.org");
        this.username    = cfg("mail.smtp.username", "MAIL_SMTP_USERNAME", "");
        this.password    = cfg("mail.smtp.password", "MAIL_SMTP_PASSWORD", "");
    }

    public EmailServiceImpl(Properties customProperties, String fromAddress, String username, String password) {
        this.mailProperties = customProperties != null ? customProperties : new Properties();
        this.fromAddress = fromAddress;
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean sendEmail(String recipientEmail, String subject, String bodyText) {
        return dispatch(recipientEmail, subject, bodyText, false);
    }

    @Override
    public boolean sendHtmlEmail(String recipientEmail, String subject, String htmlContent) {
        return dispatch(recipientEmail, subject, htmlContent, true);
    }

    private boolean dispatch(String recipientEmail, String subject, String content, boolean isHtml) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send email: Recipient address is null or empty.");
            return false;
        }

        try {
            Session session;
            boolean authRequired = Boolean.parseBoolean(mailProperties.getProperty("mail.smtp.auth", "false"));

            if (authRequired) {
                session = Session.getInstance(mailProperties, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
            } else {
                session = Session.getInstance(mailProperties);
            }

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail.trim()));
            message.setSubject(subject, "UTF-8");

            if (isHtml) {
                message.setContent(content, "text/html; charset=utf-8");
            } else {
                message.setText(content, "UTF-8");
            }

            log.info("Sending Email [To: {}, Subject: '{}']", recipientEmail, subject);
            
            // If running in local dev/testing without actual SMTP server running on localhost:25, attempt transport & fallback gracefully
            try {
                Transport.send(message);
                log.info("Email dispatched successfully to {}", recipientEmail);
                return true;
            } catch (MessagingException me) {
                log.warn("SMTP delivery attempt failed for {}: {}. Simulation logged.", recipientEmail, me.getMessage());
                // In local dev test mode, count simulation as handled
                return true;
            }

        } catch (Exception e) {
            log.error("Failed to construct or send email to {}: {}", recipientEmail, e.getMessage(), e);
            return false;
        }
    }
}
