package org.kimwanyi.sacco.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {


    @Column(nullable = false, unique = true, length = 100)
    private String name;


    @Column(length = 255)
    private String description;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",

            joinColumns =
            @JoinColumn(name = "role_id"),

            inverseJoinColumns =
            @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();



    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();



    public Role(){

    }



    public Role(String name, String description){

        this.name = name;
        this.description = description;

    }



    public void addPermission(Permission permission){

        permissions.add(permission);

        permission.getRoles().add(this);

    }



    public void removePermission(Permission permission){

        permissions.remove(permission);

        permission.getRoles().remove(this);

    }


}