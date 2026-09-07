package com.harsh.finance_project.order.repository;

import com.harsh.finance_project.order.model.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Override
    @EntityGraph(attributePaths = {"user", "asset"})
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "asset"})
    Optional<Order> findWithUserAndAssetById(Long id);

    @EntityGraph(attributePaths = {"user", "asset"})
    Page<Order> findByUserId(Long userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from TradeOrder o where o.id = :id")
    Optional<Order> findByIdForUpdate(Long id);
}
