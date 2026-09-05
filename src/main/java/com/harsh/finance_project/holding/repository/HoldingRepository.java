package com.harsh.finance_project.holding.repository;

import com.harsh.finance_project.holding.model.Holding;
import com.harsh.finance_project.user.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    List<Holding> findByUser(User user);

    Optional<Holding> findByUserIdAndAssetId(Long userId, Long assetId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Holding h where h.user.id = :userId and h.asset.id = :assetId")
    Optional<Holding> findByUserIdAndAssetIdForUpdate(Long userId, Long assetId);
}
