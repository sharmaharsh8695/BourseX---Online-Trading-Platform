package com.harsh.finance_project.portfolio.service;

import com.harsh.finance_project.common.web.PageableUtil;
import com.harsh.finance_project.holding.model.Holding;
import com.harsh.finance_project.holding.repository.HoldingRepository;
import com.harsh.finance_project.holding.repository.PortfolioValueTotals;
import com.harsh.finance_project.portfolio.dto.HoldingSummaryResponse;
import com.harsh.finance_project.portfolio.dto.PortfolioResponse;
import com.harsh.finance_project.user.repository.UserRepository;
import com.harsh.finance_project.wallet.model.Wallet;
import com.harsh.finance_project.wallet.repository.WalletRepository;
import jakarta.persistence.NoResultException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PortfolioService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final HoldingRepository holdingRepository;

    public PortfolioService(UserRepository userRepository, WalletRepository walletRepository, HoldingRepository holdingRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.holdingRepository = holdingRepository;
    }

    public PortfolioResponse getPortfolio(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NoResultException();
        }

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow(() -> new NoResultException());
        PortfolioValueTotals totals = holdingRepository.getPortfolioValueTotals(userId);
        BigDecimal totalInvestedValue = totals.getTotalInvestedValue();
        BigDecimal totalCurrentValue = totals.getTotalCurrentValue();

        return new PortfolioResponse(
                userId,
                wallet.getBalance(),
                totalInvestedValue,
                totalCurrentValue,
                totalCurrentValue.subtract(totalInvestedValue)
        );
    }

    public Page<HoldingSummaryResponse> getPortfolioHoldings(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new NoResultException();
        }

        return holdingRepository.findByUserId(userId, PageableUtil.bounded(pageable))
                .map(this::toHoldingSummary);
    }

    private HoldingSummaryResponse toHoldingSummary(Holding holding) {
        BigDecimal quantity = holding.getQuantity();
        BigDecimal averageBuyPrice = holding.getAvgPrice();
        BigDecimal currentPrice = holding.getAsset().getCurrentPrice();
        BigDecimal investedValue = quantity.multiply(averageBuyPrice);
        BigDecimal currentValue = quantity.multiply(currentPrice);
        BigDecimal profitLoss = currentValue.subtract(investedValue);

        return new HoldingSummaryResponse(holding, investedValue, currentValue, profitLoss);
    }
}
