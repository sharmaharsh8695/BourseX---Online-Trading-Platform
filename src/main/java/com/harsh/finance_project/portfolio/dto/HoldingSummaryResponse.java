package com.harsh.finance_project.portfolio.dto;

import com.harsh.finance_project.holding.model.Holding;

import java.math.BigDecimal;

public class HoldingSummaryResponse {
    public Long assetId;
    public String symbol;
    public String name;
    public BigDecimal quantity;
    public BigDecimal averageBuyPrice;
    public BigDecimal currentPrice;
    public BigDecimal investedValue;
    public BigDecimal currentValue;
    public BigDecimal profitLoss;

    public HoldingSummaryResponse(Holding holding, BigDecimal investedValue, BigDecimal currentValue, BigDecimal profitLoss) {
        this.assetId = holding.getAsset().getId();
        this.symbol = holding.getAsset().getSymbol();
        this.name = holding.getAsset().getName();
        this.quantity = holding.getQuantity();
        this.averageBuyPrice = holding.getAvgPrice();
        this.currentPrice = holding.getAsset().getCurrentPrice();
        this.investedValue = investedValue;
        this.currentValue = currentValue;
        this.profitLoss = profitLoss;
    }
}
