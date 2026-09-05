package com.harsh.finance_project.trade.dto;

import com.harsh.finance_project.order.model.OrderSide;
import com.harsh.finance_project.trade.model.Trade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeResponse {
    public Long id;
    public Long orderId;
    public Long userId;
    public Long assetId;
    public OrderSide orderSide;
    public BigDecimal quantity;
    public BigDecimal executionPrice;
    public BigDecimal totalAmount;
    public LocalDateTime executedAt;

    public TradeResponse(Trade trade) {
        this.id = trade.getId();
        this.orderId = trade.getOrder().getId();
        this.userId = trade.getUser().getId();
        this.assetId = trade.getAsset().getId();
        this.orderSide = trade.getOrderSide();
        this.quantity = trade.getQuantity();
        this.executionPrice = trade.getExecutionPrice();
        this.totalAmount = trade.getTotalAmount();
        this.executedAt = trade.getExecutedAt();
    }
}
