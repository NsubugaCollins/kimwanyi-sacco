package org.kimwanyi.sacco.security.authorization;

import jakarta.validation.ValidationException;
import org.kimwanyi.sacco.entity.User;
import org.kimwanyi.sacco.enums.PermissionName;
import org.kimwanyi.sacco.exception.AccessDeniedException;
import org.kimwanyi.sacco.repository.UserRepository;
import org.kimwanyi.sacco.util.TransactionManager;

public class AuthorizationServiceImpl {
    private final UserRepository userRepository;

    public AuthorizationServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean hasPermission(Long userId, PermissionName permission){
        return TransactionManager.execute(session -> {
            User user = userRepository.findById(session, userId).orElseThrow(()->
                    new ValidationException("user not found"));

            if(user == null){
                return false;
            }
            return user.getUserRoles().stream().filter(ur -> ur.isActive()).anyMatch(
                    ur -> ur.getRole().getRolePermissions().stream().anyMatch(
                            rp -> rp.isActive() && rp.getPermission().getName().equals(permission)
                    )
            );
        });
    }

    public void checkPermission(Long userId, PermissionName permission){
        boolean allowed = hasPermission(userId, permission);
        if(!allowed){
            throw new AccessDeniedException("You do not have permission to perform this action");
        }
    }
}
