package com.harsh.finance_project.holding.dto;

import java.math.BigDecimal;

public class UpdateHoldingRequest {
    private BigDecimal quantity;

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
