package org.kimwanyi.sacco.bean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.enums.UserStatus;
import org.kimwanyi.sacco.repositoryImpl.MemberRepositoryImpl;
import org.kimwanyi.sacco.service.EmailService;
import org.kimwanyi.sacco.serviceImpl.EmailServiceImpl;
import org.kimwanyi.sacco.util.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Named("verifyEmailBean")
@RequestScoped
public class VerifyEmailBean implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(VerifyEmailBean.class);

    private String token;
    private boolean verified = false;
    private boolean processed = false;
    private String message;
    private String errorMessage;
    private String resendIdentifier;

    public void verifyToken() {
        if (processed) return;
        processed = true;

        if (token == null || token.isBlank()) {
            this.errorMessage = "Missing or invalid verification token.";
            return;
        }

        try {
            Boolean success = TransactionManager.execute(session -> {
                MemberRepositoryImpl memberRepo = new MemberRepositoryImpl();
                Member member = memberRepo.findByVerificationToken(session, token.trim());

                if (member == null) {
                    return false;
                }

                if (member.isEmailVerified() && UserStatus.ACTIVE.equals(member.getStatus())) {
                    this.message = "Your email address is already verified. You may sign in to your account.";
                    this.verified = true;
                    return true;
                }

                if (member.getVerificationTokenExpiry() != null && member.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
                    this.errorMessage = "This verification link has expired. Please request a new verification email.";
                    this.resendIdentifier = member.getEmail();
                    return false;
                }

                member.setStatus(UserStatus.ACTIVE);
                member.setEmailVerified(true);
                member.setVerificationToken(null);
                member.setVerificationTokenExpiry(null);
                memberRepo.update(session, member);

                return true;
            });

            if (Boolean.TRUE.equals(success)) {
                this.verified = true;
                if (this.message == null) {
                    this.message = "Congratulations! Your email address has been successfully verified. You can now sign in to your SACCO account.";
                }
            } else if (this.errorMessage == null) {
                this.errorMessage = "Invalid verification token or token expired.";
            }

        } catch (Exception e) {
            log.error("Error during email verification: {}", e.getMessage(), e);
            this.errorMessage = "An unexpected error occurred during email verification. Please try again.";
        }
    }

    public String resendVerificationEmail() {
        if (resendIdentifier == null || resendIdentifier.isBlank()) {
            this.errorMessage = "Please enter your registered email address or membership number.";
            return null;
        }

        try {
            String targetEmail = resendIdentifier.trim();
            Member memberToNotify = TransactionManager.execute(session -> {
                MemberRepositoryImpl memberRepo = new MemberRepositoryImpl();
                Member member = memberRepo.findByMemberNumberOrEmailOrPhone(session, targetEmail);

                if (member == null || member.isEmailVerified()) {
                    return null;
                }

                String newToken = UUID.randomUUID().toString();
                member.setVerificationToken(newToken);
                member.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
                memberRepo.update(session, member);
                return member;
            });

            if (memberToNotify != null && memberToNotify.getEmail() != null) {
                sendVerificationEmail(memberToNotify, memberToNotify.getVerificationToken());
                this.message = "A new verification email has been sent to " + memberToNotify.getEmail() + ". Please check your inbox.";
                this.errorMessage = null;
            } else {
                this.message = "If an unverified account exists for that email, a verification link has been sent.";
            }

        } catch (Exception e) {
            log.error("Failed to resend verification email: {}", e.getMessage(), e);
            this.errorMessage = "Failed to send verification email. Please try again later.";
        }
        return null;
    }

    private void sendVerificationEmail(Member member, String token) {
        String baseUrl = getBaseAppUrl();
        String verificationUrl = baseUrl + "/verify-email.xhtml?token=" + token;

        String recipient = member.getEmail();
        String name = member.getFirstName() != null ? member.getFirstName() : "Member";

        String subject = "[Kimwanyi SACCO] Verify Your Email Address";
        String htmlBody = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"/></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px;">
                    <div style="max-width: 600px; margin: auto; background: #ffffff; border-radius: 12px; padding: 32px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
                        <h2 style="color: #0b132b; margin-top: 0;">Welcome to Kimwanyi SACCO! 🌿</h2>
                        <p style="font-size: 15px; color: #333333;">Dear %s,</p>
                        <p style="font-size: 15px; color: #555555; line-height: 1.6;">
                            Thank you for registering with Kimwanyi SACCO. Please verify your email address to activate your account and access your financial dashboard.
                        </p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background-color: #2e7d32; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px; display: inline-block;">Verify Email Address</a>
                        </div>
                        <p style="font-size: 13px; color: #777777;">
                            Or copy and paste this link into your web browser:<br/>
                            <a href="%s" style="color: #2e7d32; word-break: break-all;">%s</a>
                        </p>
                        <hr style="border: none; border-top: 1px solid #eeeeee; margin: 24px 0;"/>
                        <p style="font-size: 12px; color: #999999; text-align: center;">
                            If you did not create an account with Kimwanyi SACCO, please ignore this email.<br/>
                            This link will expire in 24 hours.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(name, verificationUrl, verificationUrl, verificationUrl);

        EmailService emailService = new EmailServiceImpl();
        emailService.sendHtmlEmail(recipient, subject, htmlBody);
    }

    public static String getBaseAppUrl() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null && context.getExternalContext() != null) {
            ExternalContext ext = context.getExternalContext();
            String scheme = ext.getRequestScheme();
            String serverName = ext.getRequestServerName();
            int serverPort = ext.getRequestServerPort();
            String contextPath = ext.getRequestContextPath();

            if ((scheme.equals("http") && serverPort == 80) || (scheme.equals("https") && serverPort == 443)) {
                return scheme + "://" + serverName + contextPath;
            } else {
                return scheme + "://" + serverName + ":" + serverPort + contextPath;
            }
        }
        return "http://localhost:8085/kimwanyi-sacco";
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public boolean isVerified() { return verified; }
    public String getMessage() { return message; }
    public String getErrorMessage() { return errorMessage; }
    public String getResendIdentifier() { return resendIdentifier; }
    public void setResendIdentifier(String resendIdentifier) { this.resendIdentifier = resendIdentifier; }
}
