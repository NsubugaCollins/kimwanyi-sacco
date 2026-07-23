package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.User;
import org.kimwanyi.sacco.repository.UserRepository;

public class UserRepositoryImpl extends GenericRepositoryImpl<User, Long> implements UserRepository {

    public UserRepositoryImpl(){
        super(User.class);
    }

    @Override
    public User findByUserName(Session session, String username){
        if (session == null) return null;
        return session.createQuery(
                "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.username = :username",
                User.class)
                .setParameter("username", username)
                .uniqueResult();
    }

    @Override
    public User findByEmail(Session session, String email){
        if (session == null) return null;
        return session.createQuery(
                "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.email = :email",
                User.class)
                .setParameter("email", email)
                .uniqueResult();
    }

    @Override
    public boolean existsByUserName(Session session, String username) {
        if (session == null) return false;
        Long count = session.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class
        ).setParameter("username", username).uniqueResult();
        return count != null && count > 0;
    }

    @Override
    public boolean existsByEmail(Session session, String email){
        if (session == null) return false;
        Long count = session.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class
        ).setParameter("email", email).uniqueResult();
        return count != null && count > 0;
    }
}
