package org.kimwanyi.sacco.service;

import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kimwanyi.sacco.audit.AuditService;
import org.kimwanyi.sacco.dto.notification.NotificationResponse;
import org.kimwanyi.sacco.dto.notification.SendNotificationRequest;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.entity.Notification;
import org.kimwanyi.sacco.entity.User;
import org.kimwanyi.sacco.enums.*;
import org.kimwanyi.sacco.event.EventPublisher;
import org.kimwanyi.sacco.event.LoanDisbursedEvent;
import org.kimwanyi.sacco.event.SavingsTransactionEvent;
import org.kimwanyi.sacco.exception.ValidationException;
import org.kimwanyi.sacco.listener.NotificationEventListener;
import org.kimwanyi.sacco.repository.MemberRepository;
import org.kimwanyi.sacco.repository.NotificationRepository;
import org.kimwanyi.sacco.repository.UserRepository;
import org.kimwanyi.sacco.serviceImpl.NotificationServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationServiceTest {

    private NotificationService notificationService;
    private DummyNotificationRepository notificationRepository;
    private DummyUserRepository userRepository;
    private DummyMemberRepository memberRepository;
    private DummyEmailService emailService;
    private EventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        notificationRepository = new DummyNotificationRepository();
        userRepository = new DummyUserRepository();
        memberRepository = new DummyMemberRepository();
        emailService = new DummyEmailService();
        AuditService dummyAuditService = new DummyAuditService();

        notificationService = new NotificationServiceImpl(
                notificationRepository,
                userRepository,
                memberRepository,
                emailService,
                dummyAuditService
        );

        eventPublisher = EventPublisher.getInstance();
        eventPublisher.clearListeners();

        NotificationEventListener listener = new NotificationEventListener(notificationService);
        listener.registerListeners(eventPublisher);

        // Pre-seed a dummy user and member
        User user = new User();
        user.setId(100L);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        userRepository.save(null, user);

        Member member = new Member();
        member.setId(200L);
        member.setFirstName("John");
        member.setLastName("Doe");
        member.setEmail("john.member@example.com");
        memberRepository.save(null, member);
    }

    @Test
    void testSendNotification_SystemChannel_Success() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(100L)
                .title("System Maintenance")
                .message("Scheduled maintenance at midnight.")
                .channel(NotificationChannel.SYSTEM)
                .type(NotificationType.SYSTEM_ALERT)
                .build();

        NotificationResponse response = notificationService.sendNotification(request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("System Maintenance", response.getTitle());
        assertEquals(NotificationStatus.SENT, response.getStatus());
        assertFalse(response.isRead());
    }

    @Test
    void testSendNotification_EmailChannel_TriggersEmailService() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(100L)
                .title("Welcome to SACCO")
                .message("Your SACCO account is now active.")
                .channel(NotificationChannel.EMAIL)
                .type(NotificationType.SYSTEM_ALERT)
                .build();

        NotificationResponse response = notificationService.sendNotification(request);

        assertNotNull(response);
        assertEquals(NotificationStatus.SENT, response.getStatus());
        assertEquals("john@example.com", response.getRecipientEmail());
        assertEquals(1, emailService.getSentEmails().size());
        assertEquals("Welcome to SACCO", emailService.getSentEmails().get(0).getSubject());
    }

    @Test
    void testMarkAsRead_UpdatesStatusAndReadAt() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(100L)
                .title("Unread Notice")
                .message("Please read this message.")
                .channel(NotificationChannel.SYSTEM)
                .build();

        NotificationResponse initialResponse = notificationService.sendNotification(request);
        assertFalse(initialResponse.isRead());

        NotificationResponse readResponse = notificationService.markAsRead(initialResponse.getId());
        assertTrue(readResponse.isRead());
        assertNotNull(readResponse.getReadAt());
    }

    @Test
    void testGetUnreadCount_ReturnsCorrectCount() {
        assertEquals(0, notificationService.getUnreadCount(100L));

        SendNotificationRequest req1 = SendNotificationRequest.builder().userId(100L).title("N1").message("M1").channel(NotificationChannel.SYSTEM).build();
        SendNotificationRequest req2 = SendNotificationRequest.builder().userId(100L).title("N2").message("M2").channel(NotificationChannel.SYSTEM).build();

        notificationService.sendNotification(req1);
        notificationService.sendNotification(req2);

        assertEquals(2, notificationService.getUnreadCount(100L));

        notificationService.markAllAsRead(100L);
        assertEquals(0, notificationService.getUnreadCount(100L));
    }

    @Test
    void testEventPublisher_SavingsTransactionEvent_TriggersNotification() {
        SavingsTransactionEvent event = new SavingsTransactionEvent(
                200L, 50L, TransactionType.DEPOSIT, new BigDecimal("500000.00"), "DEP-9999"
        );

        eventPublisher.publish(event);

        List<NotificationResponse> memberNotifs = notificationService.getMemberNotifications(200L);
        assertEquals(1, memberNotifs.size());
        assertEquals("Savings Deposit Confirmed", memberNotifs.get(0).getTitle());
        assertTrue(memberNotifs.get(0).getMessage().contains("500000.00"));
    }

    @Test
    void testEventPublisher_LoanDisbursedEvent_TriggersNotification() {
        LoanDisbursedEvent event = new LoanDisbursedEvent(
                12L, 200L, new BigDecimal("2500000.00"), 100L
        );

        eventPublisher.publish(event);

        List<NotificationResponse> memberNotifs = notificationService.getMemberNotifications(200L);
        assertEquals(1, memberNotifs.size());
        assertEquals("Loan Disbursement Notice", memberNotifs.get(0).getTitle());
        assertTrue(memberNotifs.get(0).getMessage().contains("2500000.00"));
    }

    @Test
    void testSendNotification_NullTitle_ThrowsException() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(100L)
                .title(null)
                .message("Test message")
                .build();

        assertThrows(ValidationException.class, () -> notificationService.sendNotification(request));
    }

    // Dummy In-Memory Repositories for Unit Testing
    private static class DummyNotificationRepository implements NotificationRepository {
        private final Map<Long, Notification> notifications = new HashMap<>();
        private long seq = 1L;

        @Override
        public List<Notification> findByRecipientUserId(Session session, Long userId) {
            return notifications.values().stream()
                    .filter(n -> n.getRecipientUser() != null && userId.equals(n.getRecipientUser().getId()))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Notification> findUnreadByRecipientUserId(Session session, Long userId) {
            return notifications.values().stream()
                    .filter(n -> n.getRecipientUser() != null && userId.equals(n.getRecipientUser().getId()) && !n.isReadStatus())
                    .collect(Collectors.toList());
        }

        @Override
        public List<Notification> findByRecipientMemberId(Session session, Long memberId) {
            return notifications.values().stream()
                    .filter(n -> n.getRecipientMember() != null && memberId.equals(n.getRecipientMember().getId()))
                    .collect(Collectors.toList());
        }

        @Override
        public long countUnreadByRecipientUserId(Session session, Long userId) {
            return notifications.values().stream()
                    .filter(n -> n.getRecipientUser() != null && userId.equals(n.getRecipientUser().getId()) && !n.isReadStatus())
                    .count();
        }

        @Override
        public int markAllAsReadByRecipientUserId(Session session, Long userId) {
            int count = 0;
            for (Notification n : notifications.values()) {
                if (n.getRecipientUser() != null && userId.equals(n.getRecipientUser().getId()) && !n.isReadStatus()) {
                    n.markAsRead();
                    count++;
                }
            }
            return count;
        }

        @Override
        public Notification save(Session session, Notification entity) {
            if (entity.getId() == null) entity.setId(seq++);
            notifications.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Notification update(Session session, Notification entity) {
            notifications.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<Notification> findById(Session session, Long id) {
            return Optional.ofNullable(notifications.get(id));
        }

        @Override
        public List<Notification> findAll(Session session) {
            return new ArrayList<>(notifications.values());
        }

        @Override
        public void delete(Session session, Notification entity) {
            notifications.remove(entity.getId());
        }

        @Override
        public long count(Session session) {
            return notifications.size();
        }

        @Override
        public boolean existsById(Session session, Long id) {
            return notifications.containsKey(id);
        }
    }

    private static class DummyUserRepository implements UserRepository {
        private final Map<Long, User> users = new HashMap<>();

        @Override
        public User findByUserName(Session session, String username) { return null; }

        @Override
        public User findByEmail(Session session, String email) { return null; }

        @Override
        public boolean existsByUserName(Session session, String username) { return false; }

        @Override
        public boolean existsByEmail(Session session, String email) { return false; }

        @Override
        public User save(Session session, User entity) {
            users.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public User update(Session session, User entity) {
            users.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<User> findById(Session session, Long id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public List<User> findAll(Session session) { return new ArrayList<>(users.values()); }

        @Override
        public void delete(Session session, User entity) {}

        @Override
        public long count(Session session) { return users.size(); }

        @Override
        public boolean existsById(Session session, Long id) { return users.containsKey(id); }
    }

    private static class DummyMemberRepository implements MemberRepository {
        private final Map<Long, Member> members = new HashMap<>();

        @Override
        public Member findByMemberNumber(Session session, String number) { return null; }

        @Override
        public Member findByNationalId(Session session, String nationalId) { return null; }

        @Override
        public boolean existsByMembershipNumber(Session session, String number) { return false; }

        @Override
        public boolean existsByNationalId(Session session, String nationalId) { return false; }

        @Override
        public Member save(Session session, Member entity) {
            members.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Member update(Session session, Member entity) {
            members.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<Member> findById(Session session, Long id) {
            return Optional.ofNullable(members.get(id));
        }

        @Override
        public List<Member> findAll(Session session) { return new ArrayList<>(members.values()); }

        @Override
        public void delete(Session session, Member entity) {}

        @Override
        public long count(Session session) { return members.size(); }

        @Override
        public boolean existsById(Session session, Long id) { return members.containsKey(id); }
    }

    private static class DummyEmailService implements EmailService {
        public static class SentEmail {
            private final String recipient;
            private final String subject;
            private final String body;

            public SentEmail(String recipient, String subject, String body) {
                this.recipient = recipient;
                this.subject = subject;
                this.body = body;
            }

            public String getRecipient() { return recipient; }
            public String getSubject() { return subject; }
            public String getBody() { return body; }
        }

        private final List<SentEmail> sentEmails = new ArrayList<>();

        @Override
        public boolean sendEmail(String recipientEmail, String subject, String bodyText) {
            sentEmails.add(new SentEmail(recipientEmail, subject, bodyText));
            return true;
        }

        @Override
        public boolean sendHtmlEmail(String recipientEmail, String subject, String htmlContent) {
            sentEmails.add(new SentEmail(recipientEmail, subject, htmlContent));
            return true;
        }

        public List<SentEmail> getSentEmails() { return sentEmails; }
    }

    private static class DummyAuditService implements AuditService {
        @Override
        public void logSuccess(Long userId, AuditAction action, String targetEntity, Long entityId, String details) {}

        @Override
        public void logFailure(Long userId, AuditAction action, String description) {}
    }
}
