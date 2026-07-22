package org.kimwanyi.sacco.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.kimwanyi.sacco.enums.NotificationChannel;
import org.kimwanyi.sacco.enums.NotificationStatus;
import org.kimwanyi.sacco.enums.NotificationType;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "recipient_user_id"),
        @Index(name = "idx_notification_member", columnList = "recipient_member_id"),
        @Index(name = "idx_notification_status", columnList = "status"),
        @Index(name = "idx_notification_read", columnList = "read_status")
})
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    private User recipientUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_member_id")
    private Member recipientMember;

    @Email
    @Column(name = "recipient_email", length = 150)
    private String recipientEmail;

    @NotBlank(message = "Title is required")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Message content is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel = NotificationChannel.BOTH;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type = NotificationType.SYSTEM_ALERT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "read_status", nullable = false)
    private boolean readStatus = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    public Notification() {}

    public Notification(User recipientUser, Member recipientMember, String recipientEmail, String title, String message, NotificationChannel channel, NotificationType type) {
        this.recipientUser = recipientUser;
        this.recipientMember = recipientMember;
        this.recipientEmail = recipientEmail;
        this.title = title;
        this.message = message;
        this.channel = channel != null ? channel : NotificationChannel.BOTH;
        this.type = type != null ? type : NotificationType.SYSTEM_ALERT;
        this.status = NotificationStatus.PENDING;
        this.readStatus = false;
    }

    public void markAsRead() {
        this.readStatus = true;
        this.readAt = LocalDateTime.now();
        if (this.status == NotificationStatus.PENDING) {
            this.status = NotificationStatus.READ;
        }
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsFailed(String error) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = error;
    }
}
