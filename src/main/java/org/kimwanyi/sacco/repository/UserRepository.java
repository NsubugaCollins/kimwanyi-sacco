package org.kimwanyi.sacco.repository;

import org.kimwanyi.sacco.entity.User;

public interface UserRepository extends GenericRepository<User, Long> {
    User findByUserName(String username);
    User findByEmail(String email);
    boolean existsByUserName(String username);
    boolean existsByEmail(String email);
}
