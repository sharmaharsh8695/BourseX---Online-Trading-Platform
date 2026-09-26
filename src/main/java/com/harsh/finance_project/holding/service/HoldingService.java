package com.harsh.finance_project.holding.service;

import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.common.web.PageableUtil;
import com.harsh.finance_project.common.math.FinancialPrecision;
import com.harsh.finance_project.exception.InsufficientHoldingException;
import com.harsh.finance_project.holding.model.Holding;
import com.harsh.finance_project.holding.repository.HoldingRepository;
import com.harsh.finance_project.exception.UserNotFoundException;
import com.harsh.finance_project.security.ResourceOwnershipService;
import com.harsh.finance_project.user.model.User;
import com.harsh.finance_project.user.repository.UserRepository;
import jakarta.persistence.NoResultException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class HoldingService {
    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;
    private final ResourceOwnershipService ownershipService;

    public HoldingService(HoldingRepository holdingRepository, UserRepository userRepository, ResourceOwnershipService ownershipService) {
        this.holdingRepository = holdingRepository;
        this.userRepository = userRepository;
        this.ownershipService = ownershipService;
    }

    public Holding getHoldingById(Long id) {
        Holding holding = holdingRepository.findWithUserAndAssetById(id).orElseThrow(() -> new NoResultException());
        ownershipService.requireOwner(holding.getUser().getId());
        return holding;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<Holding> getAllHoldings(Pageable pageable) {
        return holdingRepository.findAll(PageableUtil.bounded(pageable));
    }

    public Page<Holding> getHoldingsByUser(Long userId, Pageable pageable) {
        ownershipService.requireOwner(userId);
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        return holdingRepository.findByUserId(userId, PageableUtil.bounded(pageable));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordBuyExecution(User user, Asset asset, BigDecimal quantity, BigDecimal executionPrice) {
        User lockedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new UserNotFoundException(user.getId()));
        Holding holding = holdingRepository.findByUserIdAndAssetIdForUpdate(lockedUser.getId(), asset.getId())
                .orElse(null);
        if (holding == null) {
            holding = new Holding(lockedUser, asset, quantity, executionPrice);
        } else {
            BigDecimal normalizedQuantity = FinancialPrecision.quantity(quantity);
            BigDecimal normalizedPrice = FinancialPrecision.price(executionPrice);
            BigDecimal previousQuantity = holding.getQuantity();
            BigDecimal newQuantity = FinancialPrecision.quantity(previousQuantity.add(normalizedQuantity));
            BigDecimal previousValue = previousQuantity.multiply(holding.getAvgPrice());
            BigDecimal executionValue = normalizedQuantity.multiply(normalizedPrice);
            holding.setQuantity(newQuantity);
            holding.setAvgPrice(previousValue.add(executionValue)
                    .divide(newQuantity, FinancialPrecision.PRICE_SCALE, java.math.RoundingMode.HALF_UP));
        }
        holdingRepository.save(holding);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveSellQuantity(Long userId, Long assetId, BigDecimal quantity) {
        Holding holding = findLocked(userId, assetId);
        if (holding.getAvailableQuantity().compareTo(quantity) < 0) {
            throw new InsufficientHoldingException("Insufficient available holding quantity");
        }
        holding.setReservedQuantity(FinancialPrecision.quantity(
                holding.getReservedQuantity().add(FinancialPrecision.quantity(quantity))));
        holdingRepository.save(holding);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseSellReservation(Long userId, Long assetId, BigDecimal reservedQuantity) {
        Holding holding = findLocked(userId, assetId);
        if (holding.getReservedQuantity().compareTo(reservedQuantity) < 0) {
            throw new InsufficientHoldingException("Reserved holding quantity is insufficient");
        }
        holding.setReservedQuantity(FinancialPrecision.quantity(
                holding.getReservedQuantity().subtract(FinancialPrecision.quantity(reservedQuantity))));
        holdingRepository.save(holding);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void consumeSellReservation(Long userId, Long assetId, BigDecimal quantity, BigDecimal reservedQuantity) {
        Holding holding = findLocked(userId, assetId);
        if (holding.getReservedQuantity().compareTo(reservedQuantity) < 0
                || holding.getQuantity().compareTo(quantity) < 0) {
            throw new InsufficientHoldingException("Reserved holding quantity is insufficient");
        }
        holding.setReservedQuantity(FinancialPrecision.quantity(
                holding.getReservedQuantity().subtract(FinancialPrecision.quantity(reservedQuantity))));
        holding.setQuantity(FinancialPrecision.quantity(
                holding.getQuantity().subtract(FinancialPrecision.quantity(quantity))));
        holdingRepository.save(holding);
    }

    private Holding findLocked(Long userId, Long assetId) {
        return holdingRepository.findByUserIdAndAssetIdForUpdate(userId, assetId)
                .orElseThrow(() -> new InsufficientHoldingException("Holding not found for sell order"));
    }
}
