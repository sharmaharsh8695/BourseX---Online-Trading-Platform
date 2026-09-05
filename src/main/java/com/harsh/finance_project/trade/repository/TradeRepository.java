package com.harsh.finance_project.trade.repository;

import com.harsh.finance_project.trade.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByUserId(Long userId);

    List<Trade> findByOrderId(Long orderId);
}
