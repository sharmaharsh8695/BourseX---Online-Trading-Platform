package com.harsh.finance_project.asset.model;

import com.harsh.finance_project.holding.model.Holding;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "assets")
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AssetStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AssetType assetType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPrice;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "asset")
    private List<Holding> holdings;

    protected Asset(){}

    public Asset(String name, String symbol, String unit, AssetStatus status, AssetType assetType, BigDecimal currentPrice, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.name = name;
        this.symbol = symbol;
        this.unit = unit;
        this.status = status;
        this.assetType = assetType;
        this.currentPrice = currentPrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getUnit() {
        return unit;
    }

    public AssetStatus getStatus() {
        return status;
    }
    public AssetType getAssetType() {
        return assetType;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }


    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
