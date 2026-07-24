package org.kimwanyi.sacco.repository;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.Role;

public interface RoleRepository extends GenericRepository<Role, Long> {
    Role findByName(Session session, String name);
    boolean existsByName(Session session, String name);
}
