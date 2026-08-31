package com.harsh.finance_project.wallet.dto;

import com.harsh.finance_project.wallet.model.Wallet;
import com.harsh.finance_project.wallet.model.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletResponse {
    public Long id;
    public Long userId;
    public BigDecimal balance;
    public BigDecimal reservedBalance;
    public BigDecimal availableBalance;
    public WalletStatus status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public WalletResponse(Wallet wallet) {
        this.id = wallet.getId();
        this.userId = wallet.getUser().getId();
        this.balance = wallet.getBalance();
        this.reservedBalance = wallet.getReservedBalance();
        this.availableBalance = wallet.getAvailableBalance();
        this.status = wallet.getStatus();
        this.createdAt = wallet.getCreatedAt();
        this.updatedAt = wallet.getUpdatedAt();
    }
}
