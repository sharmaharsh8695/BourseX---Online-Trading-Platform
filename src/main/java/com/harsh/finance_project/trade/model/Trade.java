package com.harsh.finance_project.trade.model;

import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.order.model.Order;
import com.harsh.finance_project.order.model.OrderSide;
import com.harsh.finance_project.user.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderSide orderSide;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal executionPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    private LocalDateTime executedAt;

    protected Trade() {
    }

    public Trade(Order order, BigDecimal executionPrice, LocalDateTime executedAt) {
        this.order = order;
        this.user = order.getUser();
        this.asset = order.getAsset();
        this.orderSide = order.getOrderSide();
        this.quantity = order.getQuantity();
        this.executionPrice = executionPrice;
        this.totalAmount = order.getQuantity().multiply(executionPrice);
        this.executedAt = executedAt;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getExecutionPrice() {
        return executionPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
}
