package com.harsh.finance_project.wallet.dto;

import com.harsh.finance_project.wallet.model.WalletTransaction;
import com.harsh.finance_project.wallet.model.WalletTransactionStatus;
import com.harsh.finance_project.wallet.model.WalletTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransactionResponse {
    public Long id;
    public Long walletId;
    public Long userId;
    public WalletTransactionType transactionType;
    public WalletTransactionStatus status;
    public BigDecimal amount;
    public BigDecimal balanceAfter;
    public BigDecimal reservedBalanceAfter;
    public String reason;
    public LocalDateTime createdAt;

    public WalletTransactionResponse(WalletTransaction transaction) {
        this.id = transaction.getId();
        this.walletId = transaction.getWallet().getId();
        this.userId = transaction.getUser().getId();
        this.transactionType = transaction.getTransactionType();
        this.status = transaction.getStatus();
        this.amount = transaction.getAmount();
        this.balanceAfter = transaction.getBalanceAfter();
        this.reservedBalanceAfter = transaction.getReservedBalanceAfter();
        this.reason = transaction.getReason();
        this.createdAt = transaction.getCreatedAt();
    }
}
