package com.harsh.finance_project.user.service;

import com.harsh.finance_project.user.dto.CreateUserRequest;
import com.harsh.finance_project.user.dto.UserResponse;
import com.harsh.finance_project.user.model.User;
import com.harsh.finance_project.user.model.UserRole;
import com.harsh.finance_project.user.model.UserStatus;
import com.harsh.finance_project.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {
    private UserRepository userRepo;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, PasswordEncoder passwordEncoder){
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(CreateUserRequest dto){

        LocalDateTime now = LocalDateTime.now();

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userRepo.save(user);
        return new UserResponse(user);
    }
}
