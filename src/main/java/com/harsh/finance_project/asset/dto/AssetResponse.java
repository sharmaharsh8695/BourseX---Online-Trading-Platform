package com.harsh.finance_project.asset.dto;

import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.asset.model.AssetStatus;
import com.harsh.finance_project.asset.model.AssetType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AssetResponse {
    public Long id;

    public String name;

    public String symbol;

    public String unit;

    public AssetStatus status;

    public AssetType assetType;

    public BigDecimal currentPrice;

    public LocalDateTime createdAt;

    public LocalDateTime updatedAt;

    public AssetResponse(Asset asset){
        this.id = asset.getId();
        this.name = asset.getName();
        this.symbol = asset.getSymbol();
        this.unit = asset.getUnit();
        this.status = asset.getStatus();
        this.assetType = asset.getAssetType();
        this.currentPrice = asset.getCurrentPrice();
        this.createdAt = asset.getCreatedAt();
        this.updatedAt = asset.getUpdatedAt();
    }

}
