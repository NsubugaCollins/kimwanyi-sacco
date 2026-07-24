package org.kimwanyi.sacco.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kimwanyi.sacco.enums.NotificationChannel;
import org.kimwanyi.sacco.enums.NotificationStatus;
import org.kimwanyi.sacco.enums.NotificationType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long userId;
    private Long memberId;
    private String recipientEmail;
    private String title;
    private String message;
    private NotificationChannel channel;
    private NotificationType type;
    private NotificationStatus status;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private String errorMessage;
}
