package com.harsh.finance_project.wallet.service;

import com.harsh.finance_project.user.model.User;
import com.harsh.finance_project.user.repository.UserRepository;
import com.harsh.finance_project.wallet.dto.CreateWalletRequest;
import com.harsh.finance_project.wallet.dto.WalletAmountRequest;
import com.harsh.finance_project.wallet.dto.WalletResponse;
import com.harsh.finance_project.wallet.dto.WalletTransactionResponse;
import com.harsh.finance_project.wallet.model.Wallet;
import com.harsh.finance_project.wallet.model.WalletStatus;
import com.harsh.finance_project.wallet.model.WalletTransaction;
import com.harsh.finance_project.wallet.model.WalletTransactionType;
import com.harsh.finance_project.wallet.repository.WalletRepository;
import com.harsh.finance_project.wallet.repository.WalletTransactionRepository;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public WalletService(WalletRepository walletRepository, WalletTransactionRepository transactionRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest dto) {
        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new NoResultException());

        if (walletRepository.existsByUser(user)) {
            throw new IllegalArgumentException("Wallet already exists for user");
        }

        LocalDateTime now = LocalDateTime.now();
        Wallet wallet = new Wallet(user, now, now);
        walletRepository.save(wallet);

        return new WalletResponse(wallet);
    }

    public Wallet getWalletByUser(Long userId) {
        return walletRepository.findByUserId(userId).orElseThrow(() -> new NoResultException());
    }

    public List<WalletTransactionResponse> getTransactionsByUser(Long userId) {
        Wallet wallet = getWalletByUser(userId);

        return transactionRepository.findByWalletOrderByCreatedAtDesc(wallet).stream()
                .map(transaction -> new WalletTransactionResponse(transaction))
                .toList();
    }

    @Transactional
    public WalletResponse deposit(Long userId, WalletAmountRequest dto) {
        Wallet wallet = getWalletByUserForUpdate(userId);
        validateActive(wallet);
        validatePositiveAmount(dto.getAmount());

        wallet.setBalance(wallet.getBalance().add(dto.getAmount()));
        saveWalletMovement(wallet, WalletTransactionType.DEPOSIT, dto.getAmount(), dto.getReason());

        return new WalletResponse(wallet);
    }

    @Transactional
    public WalletResponse withdraw(Long userId, WalletAmountRequest dto) {
        Wallet wallet = getWalletByUserForUpdate(userId);
        validateActive(wallet);
        validatePositiveAmount(dto.getAmount());

        if (wallet.getAvailableBalance().compareTo(dto.getAmount()) < 0) {
            throw new IllegalArgumentException("Withdrawal amount exceeds available funds");
        }

        wallet.setBalance(wallet.getBalance().subtract(dto.getAmount()));
        saveWalletMovement(wallet, WalletTransactionType.WITHDRAWAL, dto.getAmount(), dto.getReason());

        return new WalletResponse(wallet);
    }

    @Transactional
    public WalletResponse reserveFunds(Long userId, WalletAmountRequest dto) {
        Wallet wallet = getWalletByUserForUpdate(userId);
        validateActive(wallet);
        validatePositiveAmount(dto.getAmount());

        if (wallet.getAvailableBalance().compareTo(dto.getAmount()) < 0) {
            throw new IllegalArgumentException("Reserve amount exceeds available funds");
        }

        wallet.setReservedBalance(wallet.getReservedBalance().add(dto.getAmount()));
        saveWalletMovement(wallet, WalletTransactionType.RESERVE, dto.getAmount(), dto.getReason());

        return new WalletResponse(wallet);
    }

    @Transactional
    public WalletResponse releaseReservedFunds(Long userId, WalletAmountRequest dto) {
        Wallet wallet = getWalletByUserForUpdate(userId);
        validateActive(wallet);
        validatePositiveAmount(dto.getAmount());

        if (wallet.getReservedBalance().compareTo(dto.getAmount()) < 0) {
            throw new IllegalArgumentException("Release amount exceeds reserved funds");
        }

        wallet.setReservedBalance(wallet.getReservedBalance().subtract(dto.getAmount()));
        saveWalletMovement(wallet, WalletTransactionType.RELEASE_RESERVED, dto.getAmount(), dto.getReason());

        return new WalletResponse(wallet);
    }

    @Transactional
    public WalletResponse captureReservedFunds(Long userId, WalletAmountRequest dto) {
        Wallet wallet = getWalletByUserForUpdate(userId);
        validateActive(wallet);
        validatePositiveAmount(dto.getAmount());

        if (wallet.getReservedBalance().compareTo(dto.getAmount()) < 0) {
            throw new IllegalArgumentException("Capture amount exceeds reserved funds");
        }

        wallet.setReservedBalance(wallet.getReservedBalance().subtract(dto.getAmount()));
        wallet.setBalance(wallet.getBalance().subtract(dto.getAmount()));
        saveWalletMovement(wallet, WalletTransactionType.CAPTURE_RESERVED, dto.getAmount(), dto.getReason());

        return new WalletResponse(wallet);
    }

    private Wallet getWalletByUserForUpdate(Long userId) {
        return walletRepository.findByUserIdForUpdate(userId).orElseThrow(() -> new NoResultException());
    }

    private void saveWalletMovement(Wallet wallet, WalletTransactionType type, BigDecimal amount, String reason) {
        LocalDateTime now = LocalDateTime.now();
        wallet.setUpdatedAt(now);
        walletRepository.save(wallet);
        transactionRepository.save(new WalletTransaction(wallet, type, amount, reason, now));
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void validateActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalArgumentException("Wallet is not active");
        }
    }
}
