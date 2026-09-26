package com.harsh.finance_project.user.dto;

import com.harsh.finance_project.user.model.User;

import java.time.LocalDateTime;

public class UserResponse {
    public Long id;
    public String name;
    public String email;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;


    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }

}
