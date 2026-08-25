package com.harsh.finance_project.asset.dto;

import com.harsh.finance_project.asset.model.AssetType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreateAssetRequest {
    @NotNull
    private String name;
    @NotNull
    private String symbol;
    @NotNull
    private String unit;
    @NotNull
    private AssetType assetType;
    @NotNull
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

    public void setUnit(String unit) {
        this.unit = unit;
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
