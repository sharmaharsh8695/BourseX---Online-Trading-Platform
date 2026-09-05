package com.harsh.finance_project.holding.dto;

import com.harsh.finance_project.holding.model.Holding;

import java.math.BigDecimal;

public class HoldingResponse {
    public Long id;
    public Long userId;
    public Long assetId;
    public BigDecimal quantity;
    public BigDecimal reservedQuantity;
    public BigDecimal availableQuantity;
    public BigDecimal avgPrice;

    public HoldingResponse(Holding holding) {
        this.id = holding.getId();
        this.userId = holding.getUser().getId();
        this.assetId = holding.getAsset().getId();
        this.quantity = holding.getQuantity();
        this.reservedQuantity = holding.getReservedQuantity();
        this.availableQuantity = holding.getAvailableQuantity();
        this.avgPrice = holding.getAvgPrice();
    }
}
