package org.kimwanyi.sacco.repository;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.Notification;

import java.util.List;

public interface NotificationRepository extends GenericRepository<Notification, Long> {

    List<Notification> findByRecipientUserId(Session session, Long userId);

    List<Notification> findUnreadByRecipientUserId(Session session, Long userId);

    List<Notification> findByRecipientMemberId(Session session, Long memberId);

    long countUnreadByRecipientUserId(Session session, Long userId);

    int markAllAsReadByRecipientUserId(Session session, Long userId);
}
