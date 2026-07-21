package org.kimwanyi.sacco.mapper;

import org.kimwanyi.sacco.dto.user.CreateUserRequest;
import org.kimwanyi.sacco.dto.user.UserResponse;
import org.kimwanyi.sacco.entity.User;

public interface UserMapper {
    User toEntity(CreateUserRequest request);
    UserResponse toResponse(User user);
}
