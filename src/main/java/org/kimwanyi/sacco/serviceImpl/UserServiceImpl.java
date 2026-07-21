package org.kimwanyi.sacco.serviceImpl;

import jakarta.validation.ValidationException;
import org.kimwanyi.sacco.dto.user.CreateUserRequest;
import org.kimwanyi.sacco.dto.user.UpdateUserRequest;
import org.kimwanyi.sacco.dto.user.UserResponse;
import org.kimwanyi.sacco.entity.Role;
import org.kimwanyi.sacco.entity.User;
import org.kimwanyi.sacco.enums.UserStatus;
import org.kimwanyi.sacco.mapper.UserMapper;
import org.kimwanyi.sacco.repository.RoleRepository;
import org.kimwanyi.sacco.repository.UserRepository;
import org.kimwanyi.sacco.security.PasswordEncoder;
import org.kimwanyi.sacco.service.AuditService;
import org.kimwanyi.sacco.validation.UserValidator;

import java.time.LocalDateTime;
import java.util.List;

public class UserServiceImpl {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserValidator userValidator;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditService auditService;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, UserValidator userValidator, PasswordEncoder passwordEncoder, UserMapper userMapper, AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userValidator = userValidator;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.auditService = auditService;
    }

    public UserResponse createUser(CreateUserRequest request){
        //validate DTO
        User user = userMapper.toEntity(request);
        userValidator.validate(user);

        //validate password
        userValidator.validatePassword(request.getUsername(), request.getPassword());

        //password confirmation
        if(!request.getPassword().equals(request.getConfirmPassword())){
            throw new ValidationException("Passwords do not match");
        }

        //check duplicate username
        if(userRepository.existsByUserName(request.getUsername())){
            throw new ValidationException("Username already exists");
        }

        //check duplicate email
        if(userRepository.existsByEmail(request.getEmail())){
            throw new ValidationException("Email already exists");
        }

        //encrypt password

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setStatus(UserStatus.ACTIVE);

        //assign role
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() ->
                        new ValidationException("Role not found")
                );
        user.addRole(role);

        //save
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    public UserResponse updateUser(UpdateUserRequest request){
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new ValidationException("User not found")
                );

        user.setEmail(request.getEmail());
        userRepository.update(user);
        return userMapper.toResponse(user);
    }

    public void deactivateUser(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ValidationException("User not found")
                );

        if(user == null){
            throw new ValidationException("user not found");
        }
        user.setStatus(UserStatus.INACTIVE);
        userRepository.update(user);
    }

    public void activateUser(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ValidationException("User not found")
                );

        if(user == null){
            throw new ValidationException("user not found");
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.update(user);
    }

    public void assignRole(Long userId, Long roleId){
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ValidationException("User not found")
                );

        Role role = roleRepository.findById(userId).orElseThrow(()
        -> new ValidationException("role not found"));
        if(user == null || role == null){
            throw new ValidationException("user or role not found");
        }
        user.addRole(role);
        userRepository.update(user);
    }

    public UserResponse findById(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ValidationException("User not found")
                );
        if(user == null){
            throw new ValidationException("User not found");
        }
        return userMapper.toResponse(user);
    }

    public UserResponse findByUsername(String username){
       User user = userRepository.findByUserName(username);
       if(user == null){
           throw new ValidationException("user not found");
       }
       return userMapper.toResponse(user);

    }

    public List<UserResponse> findAll(){
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }


    public void removeRole(
            Long userId,
            Long roleId
    ){

        // implement after adding removeRole()
        // helper method in User entity

    }
}
