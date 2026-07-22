package org.kimwanyi.sacco.service;

public interface EmailService {

    boolean sendEmail(String recipientEmail, String subject, String bodyText);

    boolean sendHtmlEmail(String recipientEmail, String subject, String htmlContent);
}
