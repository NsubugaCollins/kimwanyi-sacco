package org.kimwanyi.sacco.repository;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.Permission;

public interface PermissionRepository extends GenericRepository<Permission, Long> {
    Permission findByName(Session session, String name);
}
