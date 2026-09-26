package com.harsh.finance_project.trade.model;

import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.order.model.Order;
import com.harsh.finance_project.order.model.OrderSide;
import com.harsh.finance_project.user.model.User;
import com.harsh.finance_project.common.math.FinancialPrecision;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades", uniqueConstraints = {
        @UniqueConstraint(name = "uk_trade_order", columnNames = "order_id")
})
@Check(constraints = "quantity > 0 AND execution_price > 0 AND total_amount > 0")
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
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
        this.quantity = FinancialPrecision.quantity(order.getQuantity());
        this.executionPrice = FinancialPrecision.price(executionPrice);
        this.totalAmount = FinancialPrecision.settlementAmount(this.quantity, this.executionPrice);
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
