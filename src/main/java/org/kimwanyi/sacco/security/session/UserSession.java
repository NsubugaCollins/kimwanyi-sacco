package org.kimwanyi.sacco.security.session;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserSession {
    private Long userId;
    private String username;
    private Set<String> roles;
    private LocalDateTime loginTime;
    private LocalDateTime expiryTime;


    public boolean expired(){
        return expiryTime.isBefore(LocalDateTime.now());
    }

}
