package com.harsh.finance_project.user.repository;

import com.harsh.finance_project.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}