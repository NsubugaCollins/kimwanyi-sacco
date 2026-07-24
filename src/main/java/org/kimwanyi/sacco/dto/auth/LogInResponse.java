package org.kimwanyi.sacco.dto.auth;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class LogInResponse {
    private Long userId;
    private Long memberId;
    private String userType = "STAFF"; // "STAFF" or "MEMBER"
    private String username;
    private String membershipNumber;
    private String fullName;
    private String email;
    private Set<String> roles;
    private LocalDateTime loginTime;

    public LogInResponse(){

    }
}
