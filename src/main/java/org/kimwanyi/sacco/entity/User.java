package org.kimwanyi.sacco.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.kimwanyi.sacco.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_user_email", columnNames = "email")
})
public class User extends BaseEntity{

    @NotBlank(message = "Username is required")
    @Size(min = 5, max = 15)
    @Column(nullable = false, length = 30)
    private String username;

    @NotBlank(message = "Email is required")
    @Email
    @Column(nullable = false, length = 150)
    private String email;

    @NotBlank
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY
    )
    private Set<UserRole> userRoles = new HashSet<>();

    public User(){

    }

    public void incrementFailedLoginAttempts() {
        failedLoginAttempts++;
    }

    public void resetFailedLoginAttempts() {
        failedLoginAttempts = 0;
    }

    public boolean isLocked() {
        return accountLockedUntil != null &&
                accountLockedUntil.isAfter(LocalDateTime.now());
    }

    public void lockUntil(LocalDateTime time) {
        this.accountLockedUntil = time;
    }

    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    public void addRole(Role role){

        UserRole userRole = new UserRole();

        userRole.setUser(this);

        userRole.setRole(role);

        userRoles.add(userRole);

    }
}
