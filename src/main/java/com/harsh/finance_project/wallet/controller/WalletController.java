package com.harsh.finance_project.wallet.controller;

import com.harsh.finance_project.wallet.dto.CreateWalletRequest;
import com.harsh.finance_project.wallet.dto.WalletAmountRequest;
import com.harsh.finance_project.wallet.dto.WalletResponse;
import com.harsh.finance_project.wallet.dto.WalletTransactionResponse;
import com.harsh.finance_project.wallet.model.Wallet;
import com.harsh.finance_project.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/wallet")
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest dto) {
        WalletResponse res = walletService.createWallet(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/wallet/user/{userId}")
    public ResponseEntity<WalletResponse> getWalletByUser(@PathVariable Long userId) {
        Wallet wallet = walletService.getWalletByUser(userId);

        return ResponseEntity.status(HttpStatus.OK).body(new WalletResponse(wallet));
    }

    @GetMapping("/wallet/user/{userId}/transactions")
    public ResponseEntity<List<WalletTransactionResponse>> getTransactionsByUser(@PathVariable Long userId) {
        List<WalletTransactionResponse> transactions = walletService.getTransactionsByUser(userId);

        return ResponseEntity.status(HttpStatus.OK).body(transactions);
    }

    @PostMapping("/wallet/user/{userId}/deposit")
    public ResponseEntity<WalletResponse> deposit(@PathVariable Long userId, @Valid @RequestBody WalletAmountRequest dto) {
        WalletResponse res = walletService.deposit(userId, dto);

        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PostMapping("/wallet/user/{userId}/withdraw")
    public ResponseEntity<WalletResponse> withdraw(@PathVariable Long userId, @Valid @RequestBody WalletAmountRequest dto) {
        WalletResponse res = walletService.withdraw(userId, dto);

        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PostMapping("/wallet/user/{userId}/reserve")
    public ResponseEntity<WalletResponse> reserveFunds(@PathVariable Long userId, @Valid @RequestBody WalletAmountRequest dto) {
        WalletResponse res = walletService.reserveFunds(userId, dto);

        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PostMapping("/wallet/user/{userId}/release")
    public ResponseEntity<WalletResponse> releaseReservedFunds(@PathVariable Long userId, @Valid @RequestBody WalletAmountRequest dto) {
        WalletResponse res = walletService.releaseReservedFunds(userId, dto);

        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PostMapping("/wallet/user/{userId}/capture")
    public ResponseEntity<WalletResponse> captureReservedFunds(@PathVariable Long userId, @Valid @RequestBody WalletAmountRequest dto) {
        WalletResponse res = walletService.captureReservedFunds(userId, dto);

        return ResponseEntity.status(HttpStatus.OK).body(res);
    }
}
