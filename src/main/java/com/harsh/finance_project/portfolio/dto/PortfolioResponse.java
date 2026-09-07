package com.harsh.finance_project.portfolio.dto;

import java.math.BigDecimal;

public class PortfolioResponse {
    public Long userId;
    public BigDecimal cash;
    public BigDecimal totalInvestedValue;
    public BigDecimal totalCurrentValue;
    public BigDecimal totalProfitLoss;

    public PortfolioResponse(Long userId, BigDecimal cash, BigDecimal totalInvestedValue, BigDecimal totalCurrentValue, BigDecimal totalProfitLoss) {
        this.userId = userId;
        this.cash = cash;
        this.totalInvestedValue = totalInvestedValue;
        this.totalCurrentValue = totalCurrentValue;
        this.totalProfitLoss = totalProfitLoss;
    }
}
