package com.harsh.finance_project.asset.repository;

import com.harsh.finance_project.asset.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    @Override
    <S extends Asset> S save(S entity);
}
