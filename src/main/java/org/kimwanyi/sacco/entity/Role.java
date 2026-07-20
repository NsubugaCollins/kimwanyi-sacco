package org.kimwanyi.sacco.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_role_name", columnNames = "name")
        })
public class Role extends BaseEntity {

    @NotBlank(message = "Role name is required")
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String name;

    @Size(max = 255)
    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<UserRole> userRoles = new HashSet<>();

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    public Role() {
    }

}