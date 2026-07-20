package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.SessionFactory;
import org.kimwanyi.sacco.entity.UserRole;
import org.kimwanyi.sacco.repository.UserRepository;
import org.kimwanyi.sacco.repository.UserRoleRepository;

public class UserRoleRepositoryImpl extends GenericRepositoryImpl<UserRole, Long> implements UserRoleRepository {
    public UserRoleRepositoryImpl(SessionFactory sessionFactory){
        super(UserRole.class, sessionFactory);
    }
}
