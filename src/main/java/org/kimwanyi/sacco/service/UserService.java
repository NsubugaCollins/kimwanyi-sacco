package org.kimwanyi.sacco.service;

import org.kimwanyi.sacco.dto.user.CreateUserRequest;
import org.kimwanyi.sacco.dto.user.UpdateUserRequest;
import org.kimwanyi.sacco.dto.user.UserResponse;
import org.kimwanyi.sacco.entity.User;

import java.util.List;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    UserResponse updateUser(UpdateUserRequest request);
    void deactivateUser(Long userId);
    void activateUser(Long userId);
    void assignRole(Long userId, Long roleId);
    void removeRole(Long userId, Long roleId);
    UserResponse findById(Long id);
    UserResponse findByUsername(String username);
    List<UserResponse> findAll();
}
