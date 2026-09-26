package com.harsh.finance_project.order.service;

import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.asset.model.AssetStatus;
import com.harsh.finance_project.asset.repository.AssetRepository;
import com.harsh.finance_project.common.web.PageableUtil;
import com.harsh.finance_project.common.math.FinancialPrecision;
import com.harsh.finance_project.exception.AssetNotFoundException;
import com.harsh.finance_project.exception.InvalidOrderException;
import com.harsh.finance_project.exception.OrderNotFoundException;
import com.harsh.finance_project.exception.UserNotFoundException;
import com.harsh.finance_project.holding.service.HoldingService;
import com.harsh.finance_project.order.dto.CreateOrderRequest;
import com.harsh.finance_project.order.dto.OrderResponse;
import com.harsh.finance_project.order.model.Order;
import com.harsh.finance_project.order.model.OrderCategory;
import com.harsh.finance_project.order.model.OrderSide;
import com.harsh.finance_project.order.model.OrderStatus;
import com.harsh.finance_project.order.repository.OrderRepository;
import com.harsh.finance_project.security.ResourceOwnershipService;
import com.harsh.finance_project.trade.model.Trade;
import com.harsh.finance_project.trade.repository.TradeRepository;
import com.harsh.finance_project.user.model.User;
import com.harsh.finance_project.user.model.UserStatus;
import com.harsh.finance_project.user.repository.UserRepository;
import com.harsh.finance_project.wallet.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final WalletService walletService;
    private final HoldingService holdingService;
    private final ResourceOwnershipService ownershipService;

    public OrderService(OrderRepository orderRepository, TradeRepository tradeRepository, UserRepository userRepository, AssetRepository assetRepository, WalletService walletService, HoldingService holdingService, ResourceOwnershipService ownershipService) {
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.walletService = walletService;
        this.holdingService = holdingService;
        this.ownershipService = ownershipService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest dto) {
        ownershipService.requireOwner(dto.getUserId());
        validatePositiveAmount(dto.getQuantity(), "Quantity must be greater than zero");

        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new UserNotFoundException(dto.getUserId()));
        Asset asset = assetRepository.findById(dto.getAssetId()).orElseThrow(() -> new AssetNotFoundException(dto.getAssetId()));
        validateUserActive(user);
        validateAssetActive(asset);
        validatePrice(dto);
        BigDecimal price = dto.getOrderCategory() == OrderCategory.MARKET
                ? asset.getCurrentPrice()
                : dto.getRequestedPrice();
        BigDecimal quantity = FinancialPrecision.quantity(dto.getQuantity());
        BigDecimal settlementAmount = FinancialPrecision.settlementAmount(quantity, price);
        validatePositiveAmount(settlementAmount, "Order total must be at least 0.0001");

        if (dto.getOrderSide() == OrderSide.BUY) {
            user = lockUserForBuy(user.getId());
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = new Order(user, asset, dto.getOrderSide(), dto.getOrderCategory(), quantity,
                dto.getRequestedPrice() == null ? null : FinancialPrecision.price(dto.getRequestedPrice()), now);

        if (dto.getOrderSide() == OrderSide.BUY) {
            reserveBuyFunds(order, settlementAmount);
        } else {
            walletService.lockActiveWalletForOrder(order.getUser().getId());
            reserveSellHolding(order);
        }

        orderRepository.save(order);

        if (order.getOrderCategory() == OrderCategory.MARKET) {
            executeOrder(order, asset.getCurrentPrice());
        }

        return new OrderResponse(order);
    }

    public Order getOrderById(Long id) {
        Order order = orderRepository.findWithUserAndAssetById(id).orElseThrow(() -> new OrderNotFoundException(id));
        ownershipService.requireOwner(order.getUser().getId());
        return order;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(PageableUtil.bounded(pageable));
    }

    public Page<Order> getOrdersByUser(Long userId, Pageable pageable) {
        ownershipService.requireOwner(userId);
        return orderRepository.findByUserId(userId, PageableUtil.bounded(pageable));
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findByIdForUpdate(id).orElseThrow(() -> new OrderNotFoundException(id));
        ownershipService.requireOwner(order.getUser().getId());

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderException("Only pending orders can be cancelled");
        }

        if (order.getOrderSide() == OrderSide.BUY) {
            releaseBuyFunds(order);
        } else {
            releaseSellHolding(order);
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        return new OrderResponse(order);
    }

    @Transactional
    public OrderResponse executeOrder(Long id) {
        Order order = orderRepository.findByIdForUpdate(id).orElseThrow(() -> new OrderNotFoundException(id));
        ownershipService.requireOwner(order.getUser().getId());

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderException("Only pending orders can be executed");
        }

        validateUserActive(order.getUser());
        validateAssetActive(order.getAsset());
        if (order.getOrderSide() == OrderSide.BUY) {
            lockUserForBuy(order.getUser().getId());
        }
        executeOrder(order, getOrderPrice(order));

        return new OrderResponse(order);
    }

    private void executeOrder(Order order, BigDecimal executionPrice) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderException("Only pending orders can be executed");
        }

        LocalDateTime now = LocalDateTime.now();
        Trade trade = new Trade(order, executionPrice, now);
        tradeRepository.save(trade);

        if (order.getOrderSide() == OrderSide.BUY) {
            executeBuy(order, executionPrice, now);
        } else {
            executeSell(order, executionPrice, now);
        }

        order.setStatus(OrderStatus.EXECUTED);
        order.setUpdatedAt(now);
        order.setExecutedAt(now);
        orderRepository.save(order);
    }

    private void reserveBuyFunds(Order order, BigDecimal requiredAmount) {
        walletService.reserveOrderFunds(order.getUser().getId(), requiredAmount);
        order.setReservedAmount(requiredAmount);
    }

    private void releaseBuyFunds(Order order) {
        BigDecimal amount = order.getReservedAmount();
        walletService.releaseOrderFunds(order.getUser().getId(), amount);
        order.setReservedAmount(BigDecimal.ZERO);
    }

    private void executeBuy(Order order, BigDecimal executionPrice, LocalDateTime now) {
        BigDecimal totalAmount = FinancialPrecision.settlementAmount(order.getQuantity(), executionPrice);
        walletService.captureOrderFunds(order.getUser().getId(), totalAmount, order.getReservedAmount());
        holdingService.recordBuyExecution(order.getUser(), order.getAsset(), order.getQuantity(), executionPrice);
        order.setReservedAmount(BigDecimal.ZERO);
        order.setUpdatedAt(now);
    }

    private void reserveSellHolding(Order order) {
        holdingService.reserveSellQuantity(order.getUser().getId(), order.getAsset().getId(), order.getQuantity());
        order.setReservedQuantity(order.getQuantity());
    }

    private void releaseSellHolding(Order order) {
        holdingService.releaseSellReservation(order.getUser().getId(), order.getAsset().getId(),
                order.getReservedQuantity());
        order.setReservedQuantity(BigDecimal.ZERO);
    }

    private void executeSell(Order order, BigDecimal executionPrice, LocalDateTime now) {
        walletService.lockActiveWalletForOrder(order.getUser().getId());
        holdingService.consumeSellReservation(order.getUser().getId(), order.getAsset().getId(),
                order.getQuantity(), order.getReservedQuantity());

        BigDecimal totalAmount = FinancialPrecision.settlementAmount(order.getQuantity(), executionPrice);
        walletService.creditTradeSale(order.getUser().getId(), totalAmount);

        order.setReservedQuantity(BigDecimal.ZERO);
        order.setUpdatedAt(now);
    }

    private BigDecimal getOrderPrice(Order order) {
        if (order.getOrderCategory() == OrderCategory.MARKET) {
            return order.getAsset().getCurrentPrice();
        }

        return order.getRequestedPrice();
    }

    private User lockUserForBuy(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void validatePrice(CreateOrderRequest dto) {
        if (dto.getOrderCategory() == OrderCategory.LIMIT) {
            validatePositiveAmount(dto.getRequestedPrice(), "Requested price must be greater than zero for limit orders");
        }
    }

    private void validatePositiveAmount(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException(message);
        }
    }

    private void validateUserActive(User user) {
        if (user.getStatus() != null && user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidOrderException("User is not active");
        }
    }

    private void validateAssetActive(Asset asset) {
        if (asset.getStatus() != AssetStatus.ACTIVE) {
            throw new InvalidOrderException("Asset is not active");
        }
        if (asset.getCurrentPrice() == null || asset.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Asset price must be greater than zero");
        }
    }
}
