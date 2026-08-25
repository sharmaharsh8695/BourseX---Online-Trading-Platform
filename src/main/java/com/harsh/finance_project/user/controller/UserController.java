package com.harsh.finance_project.user.controller;

import com.harsh.finance_project.user.dto.CreateUserRequest;
import com.harsh.finance_project.user.dto.UserResponse;
import com.harsh.finance_project.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {
    private UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/user")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest dto){
        UserResponse res = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
}
