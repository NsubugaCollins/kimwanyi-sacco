package org.kimwanyi.sacco.service;

import org.kimwanyi.sacco.entity.User;

import java.util.List;

public interface UserService {
    User createUser(User user);
    User updateUser(User user);
    void deactivateUser(Long userId);
    void activateUser(Long userId);
    void assignRole(Long userId, Long roleId);
    void removeRole(Long userId, Long roleId);
    User findById(Long id);
    User findByUsername(String username);
    List<User> findAll();
}
