package com.harsh.finance_project.holding.service;

import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.asset.repository.AssetRepository;
import com.harsh.finance_project.common.web.PageableUtil;
import com.harsh.finance_project.holding.dto.CreateHoldingRequest;
import com.harsh.finance_project.holding.dto.HoldingResponse;
import com.harsh.finance_project.holding.dto.UpdateHoldingRequest;
import com.harsh.finance_project.holding.model.Holding;
import com.harsh.finance_project.holding.repository.HoldingRepository;
import com.harsh.finance_project.exception.AssetNotFoundException;
import com.harsh.finance_project.exception.UserNotFoundException;
import com.harsh.finance_project.security.ResourceOwnershipService;
import com.harsh.finance_project.user.model.User;
import com.harsh.finance_project.user.repository.UserRepository;
import jakarta.persistence.NoResultException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class HoldingService {
    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final ResourceOwnershipService ownershipService;

    public HoldingService(HoldingRepository holdingRepository, UserRepository userRepository, AssetRepository assetRepository, ResourceOwnershipService ownershipService) {
        this.holdingRepository = holdingRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.ownershipService = ownershipService;
    }

    public HoldingResponse createHolding(CreateHoldingRequest dto) {
        ownershipService.requireOwner(dto.getUserId());
        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new UserNotFoundException(dto.getUserId()));
        Asset asset = assetRepository.findById(dto.getAssetId()).orElseThrow(() -> new AssetNotFoundException(dto.getAssetId()));

        Holding holding = new Holding(user, asset, dto.getQuantity(),asset.getCurrentPrice());

        holdingRepository.save(holding);

        return new HoldingResponse(holding);
    }

    public Holding getHoldingById(Long id) {
        Holding holding = holdingRepository.findWithUserAndAssetById(id).orElseThrow(() -> new NoResultException());
        ownershipService.requireOwner(holding.getUser().getId());
        return holding;
    }

    public Holding updateHolding(Long id, UpdateHoldingRequest dto) {
        Holding holding = holdingRepository.findById(id).orElseThrow(() -> new NoResultException());
        ownershipService.requireOwner(holding.getUser().getId());

        if (dto.getQuantity() != null) {
            holding.setQuantity(dto.getQuantity());
        }

        holdingRepository.save(holding);

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

    public void deleteHolding(Long id) {
        Holding holding = holdingRepository.findById(id).orElseThrow(() -> new NoResultException());
        ownershipService.requireOwner(holding.getUser().getId());

        holdingRepository.delete(holding);
    }
}
