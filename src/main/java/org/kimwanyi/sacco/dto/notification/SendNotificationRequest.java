package org.kimwanyi.sacco.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kimwanyi.sacco.enums.NotificationChannel;
import org.kimwanyi.sacco.enums.NotificationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    private Long userId;
    private Long memberId;
    private String recipientEmail;
    private String title;
    private String message;
    private NotificationChannel channel;
    private NotificationType type;
}
