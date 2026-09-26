package com.harsh.finance_project.holding.model;

import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.user.model.User;
import com.harsh.finance_project.common.math.FinancialPrecision;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Table(name = "holdings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_holding_user_asset", columnNames = {"user_id", "asset_id"})
})
@Check(constraints = "quantity >= 0 AND (reserved_quantity IS NULL OR (reserved_quantity >= 0 AND reserved_quantity <= quantity))")
public class Holding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal reservedQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal avgPrice;

    protected Holding() {
    }

    public Holding(User user, Asset asset, BigDecimal quantity, BigDecimal avgPrice) {
        this.user = user;
        this.asset = asset;
        this.quantity = FinancialPrecision.quantity(quantity);
        this.reservedQuantity = BigDecimal.ZERO;
        this.avgPrice = FinancialPrecision.price(avgPrice);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getReservedQuantity() {
        return reservedQuantity == null ? BigDecimal.ZERO : reservedQuantity;
    }

    public BigDecimal getAvailableQuantity() {
        return getQuantity().subtract(getReservedQuantity());
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = FinancialPrecision.quantity(quantity);
    }

    public void setReservedQuantity(BigDecimal reservedQuantity) {
        this.reservedQuantity = FinancialPrecision.quantity(reservedQuantity);
    }

    public BigDecimal getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(BigDecimal avgPrice) {
        this.avgPrice = FinancialPrecision.price(avgPrice);
    }
}
