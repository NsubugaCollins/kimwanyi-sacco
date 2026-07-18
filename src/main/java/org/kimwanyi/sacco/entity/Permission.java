package org.kimwanyi.sacco.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {


    @Column(nullable = false, unique = true, length = 100)
    private String name;


    @Column(length = 1000)
    private String description;


    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();


    public Permission(){

    }


    public Permission(String name, String description){

        this.name = name;
        this.description = description;

    }


}