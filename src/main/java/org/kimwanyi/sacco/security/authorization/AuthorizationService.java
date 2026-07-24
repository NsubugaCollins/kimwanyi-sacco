package org.kimwanyi.sacco.security.authorization;

import org.kimwanyi.sacco.enums.PermissionName;

public interface AuthorizationService {
    boolean hasPermission(Long userId, PermissionName permission);
    void checkPermission(Long userId, PermissionName permission);
}
