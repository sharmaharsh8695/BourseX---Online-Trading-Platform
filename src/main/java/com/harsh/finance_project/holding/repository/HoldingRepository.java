package com.harsh.finance_project.holding.repository;

import com.harsh.finance_project.holding.model.Holding;
import com.harsh.finance_project.user.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    @Override
    @EntityGraph(attributePaths = {"user", "asset"})
    Page<Holding> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "asset"})
    Optional<Holding> findWithUserAndAssetById(Long id);

    List<Holding> findByUser(User user);

    @EntityGraph(attributePaths = {"user", "asset"})
    Page<Holding> findByUserId(Long userId, Pageable pageable);

    Optional<Holding> findByUserIdAndAssetId(Long userId, Long assetId);

    @Query("""
            select coalesce(sum(h.quantity * h.avgPrice), 0) as totalInvestedValue,
                   coalesce(sum(h.quantity * h.asset.currentPrice), 0) as totalCurrentValue
            from Holding h
            where h.user.id = :userId
            """)
    PortfolioValueTotals getPortfolioValueTotals(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Holding h where h.user.id = :userId and h.asset.id = :assetId")
    Optional<Holding> findByUserIdAndAssetIdForUpdate(Long userId, Long assetId);
}
