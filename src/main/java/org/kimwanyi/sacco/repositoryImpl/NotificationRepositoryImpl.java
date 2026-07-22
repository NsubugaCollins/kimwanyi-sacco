package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.Notification;
import org.kimwanyi.sacco.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationRepositoryImpl extends GenericRepositoryImpl<Notification, Long> implements NotificationRepository {

    public NotificationRepositoryImpl() {
        super(Notification.class);
    }

    @Override
    public List<Notification> findByRecipientUserId(Session session, Long userId) {
        return session.createQuery(
                "FROM Notification n WHERE n.recipientUser.id = :userId ORDER BY n.createdAt DESC", Notification.class
        ).setParameter("userId", userId).getResultList();
    }

    @Override
    public List<Notification> findUnreadByRecipientUserId(Session session, Long userId) {
        return session.createQuery(
                "FROM Notification n WHERE n.recipientUser.id = :userId AND n.readStatus = false ORDER BY n.createdAt DESC", Notification.class
        ).setParameter("userId", userId).getResultList();
    }

    @Override
    public List<Notification> findByRecipientMemberId(Session session, Long memberId) {
        return session.createQuery(
                "FROM Notification n WHERE n.recipientMember.id = :memberId ORDER BY n.createdAt DESC", Notification.class
        ).setParameter("memberId", memberId).getResultList();
    }

    @Override
    public long countUnreadByRecipientUserId(Session session, Long userId) {
        Long count = session.createQuery(
                "SELECT COUNT(n) FROM Notification n WHERE n.recipientUser.id = :userId AND n.readStatus = false", Long.class
        ).setParameter("userId", userId).uniqueResult();
        return count != null ? count : 0L;
    }

    @Override
    public int markAllAsReadByRecipientUserId(Session session, Long userId) {
        return session.createMutationQuery(
                "UPDATE Notification n SET n.readStatus = true, n.readAt = :now WHERE n.recipientUser.id = :userId AND n.readStatus = false"
        ).setParameter("now", LocalDateTime.now())
         .setParameter("userId", userId)
         .executeUpdate();
    }
}
