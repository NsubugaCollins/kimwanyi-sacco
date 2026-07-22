package org.kimwanyi.sacco.serviceImpl;

import org.kimwanyi.sacco.audit.AuditService;
import org.kimwanyi.sacco.dto.notification.NotificationResponse;
import org.kimwanyi.sacco.dto.notification.SendNotificationRequest;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.entity.Notification;
import org.kimwanyi.sacco.entity.User;
import org.kimwanyi.sacco.enums.AuditAction;
import org.kimwanyi.sacco.enums.NotificationChannel;

import org.kimwanyi.sacco.exception.ValidationException;
import org.kimwanyi.sacco.repository.MemberRepository;
import org.kimwanyi.sacco.repository.NotificationRepository;
import org.kimwanyi.sacco.repository.UserRepository;
import org.kimwanyi.sacco.service.EmailService;
import org.kimwanyi.sacco.service.NotificationService;
import org.kimwanyi.sacco.util.TransactionManager;

import java.util.List;
import java.util.stream.Collectors;

public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            MemberRepository memberRepository,
            EmailService emailService,
            AuditService auditService
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    @Override
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        validateRequest(request);

        return TransactionManager.execute(session -> {
            User user = null;
            if (request.getUserId() != null) {
                user = userRepository.findById(session, request.getUserId()).orElse(null);
            }

            Member member = null;
            if (request.getMemberId() != null) {
                member = memberRepository.findById(session, request.getMemberId()).orElse(null);
            }

            String recipientEmail = request.getRecipientEmail();
            if ((recipientEmail == null || recipientEmail.isBlank()) && user != null) {
                recipientEmail = user.getEmail();
            }
            if ((recipientEmail == null || recipientEmail.isBlank()) && member != null) {
                recipientEmail = member.getEmail();
            }

            Notification notification = new Notification(
                    user,
                    member,
                    recipientEmail,
                    request.getTitle().trim(),
                    request.getMessage().trim(),
                    request.getChannel(),
                    request.getType()
            );

            Notification savedNotification = notificationRepository.save(session, notification);

            // Attempt Email Sending if channel demands email
            NotificationChannel channel = savedNotification.getChannel();
            if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH) {
                if (recipientEmail != null && !recipientEmail.isBlank()) {
                    boolean emailSent = emailService.sendEmail(recipientEmail, savedNotification.getTitle(), savedNotification.getMessage());
                    if (emailSent) {
                        savedNotification.markAsSent();
                    } else {
                        savedNotification.markAsFailed("Email dispatch failed");
                    }
                } else {
                    savedNotification.markAsFailed("No recipient email provided");
                }
                notificationRepository.update(session, savedNotification);
            } else if (channel == NotificationChannel.SYSTEM) {
                savedNotification.markAsSent();
                notificationRepository.update(session, savedNotification);
            }

            if (auditService != null) {
                auditService.logSuccess(
                        user != null ? user.getId() : null,
                        AuditAction.SEND_NOTIFICATION,
                        "Notification",
                        savedNotification.getId(),
                        "Notification dispatched via " + channel
                );
            }

            return toResponse(savedNotification);
        });
    }

    @Override
    public NotificationResponse findById(Long id) {
        if (id == null) {
            throw new ValidationException("Notification ID is required.");
        }
        return TransactionManager.execute(session -> {
            Notification notification = notificationRepository.findById(session, id)
                    .orElseThrow(() -> new ValidationException("Notification not found with ID: " + id));
            return toResponse(notification);
        });
    }

    @Override
    public List<NotificationResponse> getUserNotifications(Long userId) {
        if (userId == null) {
            throw new ValidationException("User ID is required.");
        }
        return TransactionManager.execute(session ->
                notificationRepository.findByRecipientUserId(session, userId)
                        .stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    @Override
    public List<NotificationResponse> getUnreadUserNotifications(Long userId) {
        if (userId == null) {
            throw new ValidationException("User ID is required.");
        }
        return TransactionManager.execute(session ->
                notificationRepository.findUnreadByRecipientUserId(session, userId)
                        .stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    @Override
    public List<NotificationResponse> getMemberNotifications(Long memberId) {
        if (memberId == null) {
            throw new ValidationException("Member ID is required.");
        }
        return TransactionManager.execute(session ->
                notificationRepository.findByRecipientMemberId(session, memberId)
                        .stream().map(this::toResponse).collect(Collectors.toList())
        );
    }

    @Override
    public long getUnreadCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return TransactionManager.execute(session ->
                notificationRepository.countUnreadByRecipientUserId(session, userId)
        );
    }

    @Override
    public NotificationResponse markAsRead(Long notificationId) {
        if (notificationId == null) {
            throw new ValidationException("Notification ID is required.");
        }
        return TransactionManager.execute(session -> {
            Notification notification = notificationRepository.findById(session, notificationId)
                    .orElseThrow(() -> new ValidationException("Notification not found with ID: " + notificationId));
            
            notification.markAsRead();
            Notification updated = notificationRepository.update(session, notification);

            if (auditService != null) {
                auditService.logSuccess(
                        notification.getRecipientUser() != null ? notification.getRecipientUser().getId() : null,
                        AuditAction.MARK_NOTIFICATION_READ,
                        "Notification",
                        updated.getId(),
                        "Notification marked as read"
                );
            }

            return toResponse(updated);
        });
    }

    @Override
    public int markAllAsRead(Long userId) {
        if (userId == null) {
            throw new ValidationException("User ID is required.");
        }
        return TransactionManager.execute(session ->
                notificationRepository.markAllAsReadByRecipientUserId(session, userId)
        );
    }

    private void validateRequest(SendNotificationRequest request) {
        if (request == null) {
            throw new ValidationException("SendNotificationRequest cannot be null.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ValidationException("Notification title is required.");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new ValidationException("Notification message is required.");
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        if (notification.getRecipientUser() != null) {
            response.setUserId(notification.getRecipientUser().getId());
        }
        if (notification.getRecipientMember() != null) {
            response.setMemberId(notification.getRecipientMember().getId());
        }
        response.setRecipientEmail(notification.getRecipientEmail());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setChannel(notification.getChannel());
        response.setType(notification.getType());
        response.setStatus(notification.getStatus());
        response.setRead(notification.isReadStatus());
        response.setReadAt(notification.getReadAt());
        response.setSentAt(notification.getSentAt());
        response.setCreatedAt(notification.getCreatedAt());
        response.setErrorMessage(notification.getErrorMessage());
        return response;
    }
}
