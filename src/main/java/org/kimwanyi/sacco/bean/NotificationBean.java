package org.kimwanyi.sacco.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import org.kimwanyi.sacco.audit.AuditService;
import org.kimwanyi.sacco.dto.notification.NotificationResponse;
import org.kimwanyi.sacco.dto.notification.SendNotificationRequest;
import org.kimwanyi.sacco.enums.NotificationChannel;
import org.kimwanyi.sacco.enums.NotificationType;
import org.kimwanyi.sacco.repositoryImpl.AuditRepositoryImpl;
import org.kimwanyi.sacco.repositoryImpl.MemberRepositoryImpl;
import org.kimwanyi.sacco.repositoryImpl.NotificationRepositoryImpl;
import org.kimwanyi.sacco.repositoryImpl.UserRepositoryImpl;
import org.kimwanyi.sacco.service.NotificationService;
import org.kimwanyi.sacco.serviceImpl.AuditServiceImpl;
import org.kimwanyi.sacco.serviceImpl.EmailServiceImpl;
import org.kimwanyi.sacco.serviceImpl.NotificationServiceImpl;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Named("notificationBean")
@RequestScoped
public class NotificationBean implements Serializable {

    private NotificationService notificationService;
    private SendNotificationRequest sendRequest = new SendNotificationRequest();
    private List<NotificationResponse> notifications = Collections.emptyList();
    private long unreadCount = 0;

    private String message;
    private String errorMessage;

    @PostConstruct
    public void init() {
        try {
            NotificationRepositoryImpl notifRepo = new NotificationRepositoryImpl();
            UserRepositoryImpl userRepo = new UserRepositoryImpl();
            MemberRepositoryImpl memberRepo = new MemberRepositoryImpl();
            AuditRepositoryImpl auditRepo = new AuditRepositoryImpl();
            AuditService auditService = new AuditServiceImpl(auditRepo);

            this.notificationService = new NotificationServiceImpl(
                    notifRepo, userRepo, memberRepo, new EmailServiceImpl(), auditService
            );

            loadNotifications();
        } catch (Exception e) {
            // View init
        }
    }

    public void loadNotifications() {
        if (notificationService != null) {
            try {
                this.notifications = notificationService.getUserNotifications(100L); // Default Admin User
                this.unreadCount = notificationService.getUnreadCount(100L);
            } catch (Exception e) {
                // Graceful fallback
            }
        }
    }

    public String sendNotification() {
        try {
            if (notificationService != null) {
                if (sendRequest.getChannel() == null) sendRequest.setChannel(NotificationChannel.BOTH);
                if (sendRequest.getType() == null) sendRequest.setType(NotificationType.SYSTEM_ALERT);

                notificationService.sendNotification(sendRequest);
                this.message = "Notification / Email dispatched successfully!";
                this.sendRequest = new SendNotificationRequest();
                loadNotifications();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public String markAsRead(Long notificationId) {
        try {
            if (notificationService != null && notificationId != null) {
                notificationService.markAsRead(notificationId);
                loadNotifications();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public String markAllAsRead() {
        try {
            if (notificationService != null) {
                notificationService.markAllAsRead(100L);
                this.message = "All notifications marked as read.";
                loadNotifications();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public SendNotificationRequest getSendRequest() { return sendRequest; }
    public void setSendRequest(SendNotificationRequest sendRequest) { this.sendRequest = sendRequest; }
    public List<NotificationResponse> getNotifications() { return notifications; }
    public long getUnreadCount() { return unreadCount; }
    public String getMessage() { return message; }
    public String getErrorMessage() { return errorMessage; }
}
