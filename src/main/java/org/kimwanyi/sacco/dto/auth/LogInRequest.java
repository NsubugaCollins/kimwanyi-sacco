package org.kimwanyi.sacco.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogInRequest {

    @NotBlank(message = "username is required")
    private String username;

@NotBlank(message = "password is required")
    private  String password;

    public LogInRequest(){

    }
}
