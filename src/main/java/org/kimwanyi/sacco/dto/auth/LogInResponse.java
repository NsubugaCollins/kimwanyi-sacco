package org.kimwanyi.sacco.dto.auth;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class LogInResponse {
    private Long userId;
    private String username;
    private String email;
    private Set<String> roles;
    private LocalDateTime loginTime;

    public LogInResponse(){

    }
}
