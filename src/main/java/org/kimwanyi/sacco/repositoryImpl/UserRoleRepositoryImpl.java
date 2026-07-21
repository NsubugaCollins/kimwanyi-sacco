package org.kimwanyi.sacco.repositoryImpl;

import org.kimwanyi.sacco.entity.UserRole;
import org.kimwanyi.sacco.repository.UserRoleRepository;

public class UserRoleRepositoryImpl extends GenericRepositoryImpl<UserRole, Long> implements UserRoleRepository {

    public UserRoleRepositoryImpl() {
        super(UserRole.class);
    }
}
