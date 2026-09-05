package com.harsh.finance_project.order.dto;

import com.harsh.finance_project.order.model.Order;
import com.harsh.finance_project.order.model.OrderCategory;
import com.harsh.finance_project.order.model.OrderSide;
import com.harsh.finance_project.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderResponse {
    public Long id;
    public Long userId;
    public Long assetId;
    public OrderSide orderSide;
    public OrderCategory orderCategory;
    public BigDecimal quantity;
    public BigDecimal requestedPrice;
    public BigDecimal reservedAmount;
    public BigDecimal reservedQuantity;
    public OrderStatus status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime executedAt;

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.userId = order.getUser().getId();
        this.assetId = order.getAsset().getId();
        this.orderSide = order.getOrderSide();
        this.orderCategory = order.getOrderCategory();
        this.quantity = order.getQuantity();
        this.requestedPrice = order.getRequestedPrice();
        this.reservedAmount = order.getReservedAmount();
        this.reservedQuantity = order.getReservedQuantity();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
        this.executedAt = order.getExecutedAt();
    }
}
