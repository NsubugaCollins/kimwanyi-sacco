package org.kimwanyi.sacco.mapper;

import org.kimwanyi.sacco.dto.user.CreateUserRequest;
import org.kimwanyi.sacco.dto.user.UserResponse;
import org.kimwanyi.sacco.entity.User;

public class UserMapperImpl implements UserMapper{

    public User toEntity(CreateUserRequest request){
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        return user;
    }

    public UserResponse toResponse(User user){
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setStatus(user.getStatus().name());
        response.setLastLogin(user.getLastLogin());
        return response;
    }
}
