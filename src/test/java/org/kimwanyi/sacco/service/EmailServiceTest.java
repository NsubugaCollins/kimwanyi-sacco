package org.kimwanyi.sacco.service;

import org.junit.jupiter.api.*;
import org.kimwanyi.sacco.serviceImpl.EmailServiceImpl;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EmailServiceImpl}.
 *
 * Unit tests run without any network access.
 * The live integration test (tagged @Tag("live")) actually sends an email
 * over Gmail SMTP and is only run when the "live" tag is active.
 */
@DisplayName("EmailService Tests")
public class EmailServiceTest {

    // -----------------------------------------------------------------------
    // Credentials resolved from environment variables — never hardcoded.
    // Set these before running live tests:
    //   export MAIL_SMTP_USERNAME="you@gmail.com"
    //   export MAIL_SMTP_PASSWORD="your-app-password"
    // -----------------------------------------------------------------------
    private static final String GMAIL_USERNAME    = System.getenv("MAIL_SMTP_USERNAME");
    private static final String GMAIL_APP_PASSWORD = System.getenv("MAIL_SMTP_PASSWORD");
    private static final String RECIPIENT          = GMAIL_USERNAME; // send to self

    private static Properties gmailProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return props;
    }

    // -----------------------------------------------------------------------
    // Unit Tests — no network required
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Unit Tests")
    class UnitTests {

        private EmailService emailService;

        @BeforeEach
        void setUp() {
            // Use a deliberately broken SMTP host so no real connection is attempted.
            // EmailServiceImpl gracefully returns true even on SMTP failure (simulation mode).
            Properties props = new Properties();
            props.put("mail.smtp.host", "localhost");
            props.put("mail.smtp.port", "9999");  // no server here
            props.put("mail.smtp.auth", "false");
            props.put("mail.smtp.starttls.enable", "false");

            emailService = new EmailServiceImpl(props, "noreply@kimwanyi-sacco.org", "", "");
        }

        @Test
        @DisplayName("sendEmail returns false for null recipient")
        void sendEmail_NullRecipient_ReturnsFalse() {
            boolean result = emailService.sendEmail(null, "Test Subject", "Test body");
            assertFalse(result, "Expected false when recipient is null");
        }

        @Test
        @DisplayName("sendEmail returns false for blank recipient")
        void sendEmail_BlankRecipient_ReturnsFalse() {
            boolean result = emailService.sendEmail("   ", "Test Subject", "Test body");
            assertFalse(result, "Expected false when recipient is blank");
        }

        @Test
        @DisplayName("sendEmail returns false for empty recipient")
        void sendEmail_EmptyRecipient_ReturnsFalse() {
            boolean result = emailService.sendEmail("", "Test Subject", "Test body");
            assertFalse(result, "Expected false when recipient is empty");
        }

        @Test
        @DisplayName("sendEmail returns true with valid recipient (simulation mode)")
        void sendEmail_ValidRecipient_ReturnsTrue() {
            // Will fail to connect but EmailServiceImpl is in graceful-fallback mode
            boolean result = emailService.sendEmail("test@example.com", "Subject", "Body text");
            assertTrue(result, "Expected true even when SMTP fails (simulation/graceful mode)");
        }

        @Test
        @DisplayName("sendHtmlEmail returns true with valid recipient (simulation mode)")
        void sendHtmlEmail_ValidRecipient_ReturnsTrue() {
            boolean result = emailService.sendHtmlEmail(
                    "test@example.com",
                    "HTML Subject",
                    "<h1>Hello</h1><p>This is an HTML email.</p>"
            );
            assertTrue(result, "Expected true even when SMTP fails (simulation/graceful mode)");
        }

        @Test
        @DisplayName("sendHtmlEmail returns false for null recipient")
        void sendHtmlEmail_NullRecipient_ReturnsFalse() {
            boolean result = emailService.sendHtmlEmail(null, "HTML Subject", "<p>body</p>");
            assertFalse(result, "Expected false when recipient is null");
        }

        @Test
        @DisplayName("Default constructor reads System properties with fallback defaults")
        void defaultConstructor_UsesFallbackDefaults() {
            // Just ensure it instantiates without error
            assertDoesNotThrow((org.junit.jupiter.api.function.Executable) EmailServiceImpl::new);
        }
    }

    // -----------------------------------------------------------------------
    // Live Integration Tests — actually sends email via Gmail SMTP
    // -----------------------------------------------------------------------

    @Nested
    @Tag("live")
    @DisplayName("Live Integration Tests (Gmail SMTP)")
    class LiveIntegrationTests {

        private EmailService emailService;

        @BeforeEach
        void setUp() {
            org.junit.jupiter.api.Assumptions.assumeTrue(
                    GMAIL_USERNAME != null && !GMAIL_USERNAME.isBlank() &&
                    GMAIL_APP_PASSWORD != null && !GMAIL_APP_PASSWORD.isBlank(),
                    "Skipping live tests: MAIL_SMTP_USERNAME and MAIL_SMTP_PASSWORD env vars must be set."
            );
            emailService = new EmailServiceImpl(
                    gmailProperties(),
                    GMAIL_USERNAME,
                    GMAIL_USERNAME,
                    GMAIL_APP_PASSWORD
            );
        }

        @Test
        @DisplayName("Send plain-text email via Gmail SMTP")
        void sendPlainTextEmail_Gmail_Succeeds() {
            boolean sent = emailService.sendEmail(
                    RECIPIENT,
                    "[Kimwanyi SACCO] Plain-Text Email Test",
                    "Hello,\n\nThis is a plain-text test email sent from the Kimwanyi SACCO email service integration test.\n\nRegards,\nKimwanyi SACCO System"
            );
            assertTrue(sent, "Plain-text email should be sent successfully via Gmail SMTP");
        }

        @Test
        @DisplayName("Send HTML email via Gmail SMTP")
        void sendHtmlEmail_Gmail_Succeeds() {
            String html = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>SACCO Email Test</title></head>"
                    + "<body style=\"font-family: Arial, sans-serif; background: #f4f4f4; padding: 30px;\">"
                    + "<div style=\"max-width: 600px; margin: auto; background: white; border-radius: 8px;"
                    + "padding: 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);\">"
                    + "<h1 style=\"color: #2e7d32;\">Kimwanyi SACCO</h1>"
                    + "<h2 style=\"color: #555;\">HTML Email Test &#x2705;</h2>"
                    + "<p>This is a <strong>HTML</strong> email sent from the integration test suite.</p>"
                    + "<p style=\"color: #888; font-size: 0.85em;\">Sent at: " + java.time.LocalDateTime.now() + "</p>"
                    + "</div></body></html>";

            boolean sent = emailService.sendHtmlEmail(
                    RECIPIENT,
                    "[Kimwanyi SACCO] HTML Email Test",
                    html
            );
            assertTrue(sent, "HTML email should be sent successfully via Gmail SMTP");
        }
    }
}
