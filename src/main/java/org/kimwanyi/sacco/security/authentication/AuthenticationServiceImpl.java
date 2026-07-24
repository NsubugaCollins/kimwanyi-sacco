package org.kimwanyi.sacco.security.authentication;

import org.kimwanyi.sacco.dto.auth.LogInRequest;
import org.kimwanyi.sacco.dto.auth.LogInResponse;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.entity.User;
import org.kimwanyi.sacco.exception.AccountLockedException;
import org.kimwanyi.sacco.exception.AuthenticationException;
import org.kimwanyi.sacco.repository.MemberRepository;
import org.kimwanyi.sacco.repository.UserRepository;
import org.kimwanyi.sacco.repositoryImpl.MemberRepositoryImpl;
import org.kimwanyi.sacco.security.PasswordEncoder;
import org.kimwanyi.sacco.security.SecurityConstants;
import org.kimwanyi.sacco.util.TransactionManager;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, new MemberRepositoryImpl(), passwordEncoder);
    }

    public AuthenticationServiceImpl(UserRepository userRepository, MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LogInResponse login(LogInRequest request){
        return TransactionManager.execute(session -> {
            String identifier = request != null && request.getUsername() != null ? request.getUsername().trim() : "";
            String rawPassword = request != null ? request.getPassword() : "";

            // 1. Try Staff User authentication (from 'users' table)
            User user = userRepository.findByUserName(session, identifier);
            if (user == null) {
                user = userRepository.findByEmail(session, identifier);
            }

            if (user != null) {
                if (user.isLocked()) {
                    throw new AccountLockedException("Account temporarily locked");
                }
                if (org.kimwanyi.sacco.enums.UserStatus.PENDING_VERIFICATION.equals(user.getStatus()) || !user.isEmailVerified()) {
                    throw new AuthenticationException("Your email address has not been verified. Please check your inbox and verify your email before signing in.");
                }
                boolean validPassword = passwordEncoder.matches(rawPassword, user.getPasswordHash());
                if (!validPassword) {
                    user.increaseFailedAttempts();
                    if (user.hasReachedMaxAttempts(SecurityConstants.MAX_LOGIN_ATTEMPTS)) {
                        user.lockAccount(SecurityConstants.LOCK_DURATION_MINUTES);
                    }
                    userRepository.update(session, user);
                    throw new AuthenticationException("Invalid username or password");
                }
                user.resetFailedLoginAttempts();
                user.updateLastLogin();
                userRepository.update(session, user);
                return createStaffResponse(user);
            }

            // 2. Try Customer Member authentication (from 'members' table)
            if (memberRepository != null) {
                Member member = memberRepository.findByMemberNumberOrEmailOrPhone(session, identifier);
                if (member != null) {
                    if (org.kimwanyi.sacco.enums.UserStatus.PENDING_VERIFICATION.equals(member.getStatus()) || !member.isEmailVerified()) {
                        throw new AuthenticationException("Your email address has not been verified. Please check your inbox for the verification link before signing in.");
                    }
                    boolean validPass = member.getPasswordHash() != null &&
                            passwordEncoder.matches(rawPassword, member.getPasswordHash());
                    
                    // Fallback for initial member logins before password hash is set
                    if (!validPass && member.getPasswordHash() == null) {
                        validPass = true;
                        member.setPasswordHash(passwordEncoder.encode(rawPassword));
                        session.merge(member);
                    }

                    if (!validPass) {
                        throw new AuthenticationException("Invalid credentials for member account");
                    }
                    return createMemberResponse(member);
                }
            }

            throw new AuthenticationException("Invalid username or password");
        });
    }

    public LogInResponse createStaffResponse(User user){
        LogInResponse response = new LogInResponse();
        response.setUserId(user.getId());
        response.setUserType("STAFF");
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getUsername());
        
        Set<String> roles = new HashSet<>();
        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            user.getUserRoles().forEach(ur -> {
                if (ur.getRole() != null && ur.getRole().getName() != null) {
                    roles.add(ur.getRole().getName().toUpperCase());
                }
            });
        }
        if (roles.isEmpty()) {
            roles.add("STAFF");
        }
        response.setRoles(roles);
        response.setLoginTime(LocalDateTime.now());
        return response;
    }

    public LogInResponse createMemberResponse(Member member){
        LogInResponse response = new LogInResponse();
        response.setMemberId(member.getId());
        response.setUserId(member.getId());
        response.setUserType("MEMBER");
        response.setUsername(member.getMembershipNumber());
        response.setMembershipNumber(member.getMembershipNumber());
        response.setEmail(member.getEmail());
        String name = ((member.getFirstName() != null ? member.getFirstName() : "") + " " +
                       (member.getLastName() != null ? member.getLastName() : "")).trim();
        response.setFullName(name.isEmpty() ? member.getMembershipNumber() : name);

        Set<String> roles = new HashSet<>();
        roles.add("MEMBER");
        response.setRoles(roles);
        response.setLoginTime(LocalDateTime.now());
        return response;
    }

    @Override
    public void logout(Long userId){

    }
}
