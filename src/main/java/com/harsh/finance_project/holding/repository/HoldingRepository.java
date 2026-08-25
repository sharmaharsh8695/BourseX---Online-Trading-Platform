package com.harsh.finance_project.holding.repository;

import com.harsh.finance_project.holding.model.Holding;
import com.harsh.finance_project.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    List<Holding> findByUser(User user);
}
