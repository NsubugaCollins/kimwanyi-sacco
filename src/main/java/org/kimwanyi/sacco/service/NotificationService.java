package org.kimwanyi.sacco.service;

import org.kimwanyi.sacco.dto.notification.NotificationResponse;
import org.kimwanyi.sacco.dto.notification.SendNotificationRequest;

import java.util.List;

public interface NotificationService {

    NotificationResponse sendNotification(SendNotificationRequest request);

    NotificationResponse findById(Long id);

    List<NotificationResponse> getUserNotifications(Long userId);

    List<NotificationResponse> getUnreadUserNotifications(Long userId);

    List<NotificationResponse> getMemberNotifications(Long memberId);

    long getUnreadCount(Long userId);

    NotificationResponse markAsRead(Long notificationId);

    int markAllAsRead(Long userId);
}
