package org.kimwanyi.sacco.security.authentication;

import org.kimwanyi.sacco.dto.auth.LogInRequest;
import org.kimwanyi.sacco.dto.auth.LogInResponse;
import org.kimwanyi.sacco.entity.User;
import org.kimwanyi.sacco.exception.AccountLockedException;
import org.kimwanyi.sacco.exception.AuthenticationException;
import org.kimwanyi.sacco.repository.UserRepository;
import org.kimwanyi.sacco.security.PasswordEncoder;
import org.kimwanyi.sacco.security.SecurityConstants;
import org.kimwanyi.sacco.util.TransactionManager;

import java.time.LocalDateTime;

public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LogInResponse login(LogInRequest request){
        return TransactionManager.execute(session -> {
            User user = userRepository.findByUserName(session, request.getUsername());
            if(user == null){
                throw new AuthenticationException("Invalid username or password");
            }
            if(user.isLocked()){
                throw new AccountLockedException("Account temporarily locked");
            }
            boolean validPassword = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
            if(!validPassword){
                user.increaseFailedAttempts();
                if(user.hasReachedMaxAttempts(SecurityConstants.MAX_LOGIN_ATTEMPTS)){
                    user.lockAccount(SecurityConstants.LOCK_DURATION_MINUTES);
                }
                userRepository.update(session, user);
                throw new AuthenticationException("Invalid username or password");
            }
            user.resetFailedLoginAttempts();
            user.updateLastLogin();
            userRepository.update(session, user);
            return createResponse(user);
        });
    }

    public LogInResponse createResponse(User user){
        LogInResponse response = new LogInResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setLoginTime(LocalDateTime.now());
        return response;
    }

    @Override
    public void logout(Long userId){

    }
}
