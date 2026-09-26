package com.harsh.finance_project.wallet.model;

import com.harsh.finance_project.user.model.User;
import com.harsh.finance_project.common.math.FinancialPrecision;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Check(constraints = "amount > 0 AND balance_after >= 0 AND reserved_balance_after >= 0 AND balance_after >= reserved_balance_after")
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
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
        this.amount = FinancialPrecision.money(amount);
        this.balanceAfter = FinancialPrecision.money(wallet.getBalance());
        this.reservedBalanceAfter = FinancialPrecision.money(wallet.getReservedBalance());
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
