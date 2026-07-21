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
import org.kimwanyi.sacco.util.TransactionManager;
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
        User user = userMapper.toEntity(request);
        userValidator.validate(user);
        userValidator.validatePassword(request.getUsername(), request.getPassword());

        if(!request.getPassword().equals(request.getConfirmPassword())){
            throw new ValidationException("Passwords do not match");
        }

        return TransactionManager.execute(session -> {
            if(userRepository.existsByUserName(session, request.getUsername())){
                throw new ValidationException("Username already exists");
            }

            if(userRepository.existsByEmail(session, request.getEmail())){
                throw new ValidationException("Email already exists");
            }

            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setPasswordChangedAt(LocalDateTime.now());
            user.setStatus(UserStatus.ACTIVE);

            Role role = roleRepository.findById(session, request.getRoleId())
                    .orElseThrow(() -> new ValidationException("Role not found"));
            user.addRole(role);

            User savedUser = userRepository.save(session, user);
            return userMapper.toResponse(savedUser);
        });
    }

    public UserResponse updateUser(UpdateUserRequest request){
        return TransactionManager.execute(session -> {
            User user = userRepository.findById(session, request.getUserId())
                    .orElseThrow(() -> new ValidationException("User not found"));

            user.setEmail(request.getEmail());
            userRepository.update(session, user);
            return userMapper.toResponse(user);
        });
    }

    public void deactivateUser(Long userId){
        TransactionManager.execute(session -> {
            User user = userRepository.findById(session, userId)
                    .orElseThrow(() -> new ValidationException("User not found"));

            user.setStatus(UserStatus.INACTIVE);
            userRepository.update(session, user);
            return null;
        });
    }

    public void activateUser(Long userId){
        TransactionManager.execute(session -> {
            User user = userRepository.findById(session, userId)
                    .orElseThrow(() -> new ValidationException("User not found"));

            user.setStatus(UserStatus.ACTIVE);
            userRepository.update(session, user);
            return null;
        });
    }

    public void assignRole(Long userId, Long roleId){
        TransactionManager.execute(session -> {
            User user = userRepository.findById(session, userId)
                    .orElseThrow(() -> new ValidationException("User not found"));

            Role role = roleRepository.findById(session, roleId)
                    .orElseThrow(() -> new ValidationException("Role not found"));

            user.addRole(role);
            userRepository.update(session, user);
            return null;
        });
    }

    public UserResponse findById(Long userId){
        return TransactionManager.execute(session -> {
            User user = userRepository.findById(session, userId)
                    .orElseThrow(() -> new ValidationException("User not found"));
            return userMapper.toResponse(user);
        });
    }

    public UserResponse findByUsername(String username){
        return TransactionManager.execute(session -> {
            User user = userRepository.findByUserName(session, username);
            if(user == null){
                throw new ValidationException("User not found");
            }
            return userMapper.toResponse(user);
        });
    }

    public List<UserResponse> findAll(){
        return TransactionManager.execute(session -> {
            return userRepository.findAll(session).stream().map(userMapper::toResponse).toList();
        });
    }

    public void removeRole(Long userId, Long roleId){
        // implement after adding removeRole() helper method in User entity
    }
}
