package org.kimwanyi.sacco.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_role",
       uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_role", columnNames = {"user_id", "role_id"})
       }
)
public class UserRole extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = true)
    private Permission permission;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "expiryDate", nullable = true)
    private LocalDateTime expiryDate;

    public UserRole(){
        this.active = true;
        this.expiryDate = LocalDateTime.now().plusYears(10);
    }

    public UserRole(User user, Role role){
        this.user = user;
        this.role = role;
        this.active = true;
        this.expiryDate = LocalDateTime.now().plusYears(10);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
}
