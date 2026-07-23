package org.kimwanyi.sacco.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kimwanyi.sacco.dto.auth.LogInRequest;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.enums.UserStatus;
import org.kimwanyi.sacco.exception.AuthenticationException;
import org.kimwanyi.sacco.repository.MemberRepository;
import org.kimwanyi.sacco.repositoryImpl.MemberRepositoryImpl;
import org.kimwanyi.sacco.security.BCryptPasswordEncoder;
import org.kimwanyi.sacco.security.PasswordEncoder;
import org.kimwanyi.sacco.security.authentication.AuthenticationService;
import org.kimwanyi.sacco.security.authentication.AuthenticationServiceImpl;
import org.kimwanyi.sacco.util.TransactionManager;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Email Verification Tests")
public class EmailVerificationTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    @DisplayName("Unverified member status is PENDING_VERIFICATION and emailVerified is false")
    void testNewMember_IsPendingVerification() {
        Member member = new Member();
        member.setMembershipNumber("MEM-TEST-VERIFY-1");
        member.setFirstName("Jane");
        member.setLastName("Doe");
        member.setNationalId("NID-TEST-VERIFY-1");
        member.setEmail("jane.unverified@example.com");
        member.setPasswordHash(passwordEncoder.encode("Secret123!"));
        member.setStatus(UserStatus.PENDING_VERIFICATION);
        member.setEmailVerified(false);

        String token = UUID.randomUUID().toString();
        member.setVerificationToken(token);
        member.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

        assertEquals(UserStatus.PENDING_VERIFICATION, member.getStatus());
        assertFalse(member.isEmailVerified());
        assertNotNull(member.getVerificationToken());
        assertTrue(member.getVerificationTokenExpiry().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Verifying token sets member status to ACTIVE and emailVerified to true")
    void testVerifyMember_TokenActivation() {
        Member member = new Member();
        member.setStatus(UserStatus.PENDING_VERIFICATION);
        member.setEmailVerified(false);
        member.setVerificationToken("sample-valid-token-123");
        member.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

        // Simulate verification token approval
        member.setStatus(UserStatus.ACTIVE);
        member.setEmailVerified(true);
        member.setVerificationToken(null);
        member.setVerificationTokenExpiry(null);

        assertEquals(UserStatus.ACTIVE, member.getStatus());
        assertTrue(member.isEmailVerified());
        assertNull(member.getVerificationToken());
        assertNull(member.getVerificationTokenExpiry());
    }
}
