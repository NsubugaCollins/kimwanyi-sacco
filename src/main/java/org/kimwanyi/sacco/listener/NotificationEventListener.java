package org.kimwanyi.sacco.listener;

import org.kimwanyi.sacco.dto.notification.SendNotificationRequest;
import org.kimwanyi.sacco.enums.NotificationChannel;
import org.kimwanyi.sacco.enums.NotificationType;
import org.kimwanyi.sacco.enums.TransactionType;
import org.kimwanyi.sacco.event.*;
import org.kimwanyi.sacco.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void registerListeners(EventPublisher publisher) {
        if (publisher == null) return;

        publisher.registerListener(SavingsTransactionEvent.class, this::handleSavingsTransaction);
        publisher.registerListener(LoanDisbursedEvent.class, this::handleLoanDisbursed);
        publisher.registerListener(LoanRepaidEvent.class, this::handleLoanRepaid);
    }

    public void handleSavingsTransaction(SavingsTransactionEvent event) {
        if (event == null) return;

        boolean isDeposit = event.getTransactionType() == TransactionType.DEPOSIT;
        String actionTitle = isDeposit ? "Savings Deposit Confirmed" : "Savings Withdrawal Notice";
        NotificationType notifType = isDeposit ? NotificationType.SAVINGS_DEPOSIT : NotificationType.SAVINGS_WITHDRAWAL;

        String message = String.format(
                "A %s of %s UGX (Ref: %s) was successfully processed for savings account #%d.",
                event.getTransactionType().name(),
                event.getAmount() != null ? event.getAmount().toPlainString() : "0.00",
                event.getReferenceNumber() != null ? event.getReferenceNumber() : "N/A",
                event.getSavingsAccountId()
        );

        SendNotificationRequest request = SendNotificationRequest.builder()
                .memberId(event.getMemberId())
                .title(actionTitle)
                .message(message)
                .channel(NotificationChannel.BOTH)
                .type(notifType)
                .build();

        try {
            notificationService.sendNotification(request);
        } catch (Exception e) {
            log.error("Failed to send notification for SavingsTransactionEvent: {}", e.getMessage(), e);
        }
    }

    public void handleLoanDisbursed(LoanDisbursedEvent event) {
        if (event == null) return;

        String title = "Loan Disbursement Notice";
        String message = String.format(
                "Congratulations! Your loan (Loan ID: %d) of %s UGX has been successfully disbursed.",
                event.getLoanId(),
                event.getPrincipalAmount() != null ? event.getPrincipalAmount().toPlainString() : "0.00"
        );

        SendNotificationRequest request = SendNotificationRequest.builder()
                .memberId(event.getMemberId())
                .title(title)
                .message(message)
                .channel(NotificationChannel.BOTH)
                .type(NotificationType.LOAN_DISBURSED)
                .build();

        try {
            notificationService.sendNotification(request);
        } catch (Exception e) {
            log.error("Failed to send notification for LoanDisbursedEvent: {}", e.getMessage(), e);
        }
    }

    public void handleLoanRepaid(LoanRepaidEvent event) {
        if (event == null) return;

        String title = "Loan Repayment Received";
        String message = String.format(
                "Thank you! Your loan repayment of %s UGX (Ref: %s) has been recorded.",
                event.getAmountPaid() != null ? event.getAmountPaid().toPlainString() : "0.00",
                event.getReferenceNumber() != null ? event.getReferenceNumber() : "N/A"
        );

        SendNotificationRequest request = SendNotificationRequest.builder()
                .memberId(event.getMemberId())
                .title(title)
                .message(message)
                .channel(NotificationChannel.BOTH)
                .type(NotificationType.LOAN_REPAYMENT)
                .build();

        try {
            notificationService.sendNotification(request);
        } catch (Exception e) {
            log.error("Failed to send notification for LoanRepaidEvent: {}", e.getMessage(), e);
        }
    }
}
