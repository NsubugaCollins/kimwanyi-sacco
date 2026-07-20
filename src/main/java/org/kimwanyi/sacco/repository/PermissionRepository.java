package org.kimwanyi.sacco.repository;

import org.kimwanyi.sacco.entity.Permission;

public interface PermissionRepository extends GenericRepository<Permission, Long> {
    Permission findByName(String name);
}
