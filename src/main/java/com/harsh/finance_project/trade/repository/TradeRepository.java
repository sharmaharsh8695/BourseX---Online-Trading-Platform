package com.harsh.finance_project.trade.repository;

import com.harsh.finance_project.trade.model.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    @Override
    @EntityGraph(attributePaths = {"order", "user", "asset"})
    Page<Trade> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"order", "user", "asset"})
    Optional<Trade> findWithOrderUserAssetById(Long id);

    @EntityGraph(attributePaths = {"order", "user", "asset"})
    Page<Trade> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "user", "asset"})
    Page<Trade> findByOrderId(Long orderId, Pageable pageable);
}
