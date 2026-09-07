package com.harsh.finance_project.order.model;

import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.user.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "TradeOrder")
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderSide orderSide;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderCategory orderCategory;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal requestedPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal reservedAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal reservedQuantity;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime executedAt;

    protected Order() {
    }

    public Order(User user, Asset asset, OrderSide orderSide, OrderCategory orderCategory, BigDecimal quantity, BigDecimal requestedPrice, LocalDateTime now) {
        this.user = user;
        this.asset = asset;
        this.orderSide = orderSide;
        this.orderCategory = orderCategory;
        this.quantity = quantity;
        this.requestedPrice = requestedPrice;
        this.reservedAmount = BigDecimal.ZERO;
        this.reservedQuantity = BigDecimal.ZERO;
        this.status = OrderStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Asset getAsset() {
        return asset;
    }

    public OrderSide getOrderSide() {
        return orderSide;
    }

    public OrderCategory getOrderCategory() {
        return orderCategory;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getRequestedPrice() {
        return requestedPrice;
    }

    public BigDecimal getReservedAmount() {
        return reservedAmount == null ? BigDecimal.ZERO : reservedAmount;
    }

    public BigDecimal getReservedQuantity() {
        return reservedQuantity == null ? BigDecimal.ZERO : reservedQuantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setReservedAmount(BigDecimal reservedAmount) {
        this.reservedAmount = reservedAmount;
    }

    public void setReservedQuantity(BigDecimal reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
