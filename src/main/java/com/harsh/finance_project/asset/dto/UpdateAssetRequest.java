package com.harsh.finance_project.asset.dto;

import com.harsh.finance_project.asset.model.AssetStatus;
import com.harsh.finance_project.asset.model.AssetType;

import java.math.BigDecimal;

public class UpdateAssetRequest {
    private String name;
    private String symbol;
    private String unit;
    private AssetStatus status;
    private AssetType assetType;
    private BigDecimal currentPrice;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getUnit() {
        return unit;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
}
