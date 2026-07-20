package org.kimwanyi.sacco.repository;

import org.kimwanyi.sacco.entity.Role;

public interface RoleRepository extends GenericRepository<Role, Long>{
    Role findByName(String name);
    boolean existsByName(String name);
}
