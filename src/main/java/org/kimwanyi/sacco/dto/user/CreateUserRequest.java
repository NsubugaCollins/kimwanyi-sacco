package org.kimwanyi.sacco.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 5, max = 15, message = "username must be between 4 and 15 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "password required")
    private String password;

    @NotBlank(message = "Confirm the password")
    private String confirmPassword;

    private Long roleId;

    public CreateUserRequest(){

    }
}
