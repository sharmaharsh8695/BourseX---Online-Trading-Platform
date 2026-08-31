package com.harsh.finance_project.wallet.model;

import com.harsh.finance_project.user.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WalletTransactionType transactionType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WalletTransactionStatus status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedBalanceAfter;

    private String reason;

    private LocalDateTime createdAt;

    protected WalletTransaction() {
    }

    public WalletTransaction(Wallet wallet, WalletTransactionType transactionType, BigDecimal amount, String reason, LocalDateTime createdAt) {
        this.wallet = wallet;
        this.user = wallet.getUser();
        this.transactionType = transactionType;
        this.status = WalletTransactionStatus.COMPLETED;
        this.amount = amount;
        this.balanceAfter = wallet.getBalance();
        this.reservedBalanceAfter = wallet.getReservedBalance();
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public User getUser() {
        return user;
    }

    public WalletTransactionType getTransactionType() {
        return transactionType;
    }

    public WalletTransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public BigDecimal getReservedBalanceAfter() {
        return reservedBalanceAfter;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
