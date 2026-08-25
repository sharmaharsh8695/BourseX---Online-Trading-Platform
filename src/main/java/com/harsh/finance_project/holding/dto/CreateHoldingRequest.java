package com.harsh.finance_project.holding.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreateHoldingRequest {
    @NotNull
    private Long userId;

    @NotNull
    private Long assetId;

    @NotNull
    private BigDecimal quantity;


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    }
