package org.kimwanyi.sacco.repository;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.User;

public interface UserRepository extends GenericRepository<User, Long> {
    User findByUserName(Session session, String username);
    User findByEmail(Session session, String email);
    boolean existsByUserName(Session session, String username);
    boolean existsByEmail(Session session, String email);
}
