package org.kimwanyi.sacco.security.authentication;

import org.kimwanyi.sacco.dto.auth.LogInRequest;
import org.kimwanyi.sacco.dto.auth.LogInResponse;

public interface AuthenticationService {
    LogInResponse login(LogInRequest request);
    void logout(Long userId);
}
