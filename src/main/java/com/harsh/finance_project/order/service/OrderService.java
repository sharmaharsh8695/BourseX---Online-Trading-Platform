package com.harsh.finance_project.order.service;

import com.harsh.finance_project.asset.model.Asset;
import com.harsh.finance_project.asset.model.AssetStatus;
import com.harsh.finance_project.asset.repository.AssetRepository;
import com.harsh.finance_project.common.web.PageableUtil;
import com.harsh.finance_project.holding.model.Holding;
import com.harsh.finance_project.holding.repository.HoldingRepository;
import com.harsh.finance_project.order.dto.CreateOrderRequest;
import com.harsh.finance_project.order.dto.OrderResponse;
import com.harsh.finance_project.order.model.Order;
import com.harsh.finance_project.order.model.OrderCategory;
import com.harsh.finance_project.order.model.OrderSide;
import com.harsh.finance_project.order.model.OrderStatus;
import com.harsh.finance_project.order.repository.OrderRepository;
import com.harsh.finance_project.trade.model.Trade;
import com.harsh.finance_project.trade.repository.TradeRepository;
import com.harsh.finance_project.user.model.User;
import com.harsh.finance_project.user.model.UserStatus;
import com.harsh.finance_project.user.repository.UserRepository;
import com.harsh.finance_project.wallet.model.Wallet;
import com.harsh.finance_project.wallet.model.WalletStatus;
import com.harsh.finance_project.wallet.model.WalletTransaction;
import com.harsh.finance_project.wallet.model.WalletTransactionType;
import com.harsh.finance_project.wallet.repository.WalletRepository;
import com.harsh.finance_project.wallet.repository.WalletTransactionRepository;
import jakarta.persistence.NoResultException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final HoldingRepository holdingRepository;

    public OrderService(OrderRepository orderRepository, TradeRepository tradeRepository, UserRepository userRepository, AssetRepository assetRepository, WalletRepository walletRepository, WalletTransactionRepository walletTransactionRepository, HoldingRepository holdingRepository) {
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.holdingRepository = holdingRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest dto) {
        validatePositiveAmount(dto.getQuantity(), "Quantity must be greater than zero");

        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new NoResultException());
        Asset asset = assetRepository.findById(dto.getAssetId()).orElseThrow(() -> new NoResultException());
        validateUserActive(user);
        validateAssetActive(asset);
        validatePrice(dto);

        LocalDateTime now = LocalDateTime.now();
        Order order = new Order(user, asset, dto.getOrderSide(), dto.getOrderCategory(), dto.getQuantity(), dto.getRequestedPrice(), now);

        if (dto.getOrderSide() == OrderSide.BUY) {
            reserveBuyFunds(order, getOrderPrice(order));
        } else {
            reserveSellHolding(order);
        }

        orderRepository.save(order);

        if (order.getOrderCategory() == OrderCategory.MARKET) {
            executeOrder(order, asset.getCurrentPrice());
        }

        return new OrderResponse(order);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findWithUserAndAssetById(id).orElseThrow(() -> new NoResultException());
    }

    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(PageableUtil.bounded(pageable));
    }

    public Page<Order> getOrdersByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, PageableUtil.bounded(pageable));
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findByIdForUpdate(id).orElseThrow(() -> new NoResultException());

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only pending orders can be cancelled");
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
        Order order = orderRepository.findByIdForUpdate(id).orElseThrow(() -> new NoResultException());

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only pending orders can be executed");
        }

        validateUserActive(order.getUser());
        validateAssetActive(order.getAsset());
        executeOrder(order, getOrderPrice(order));

        return new OrderResponse(order);
    }

    private void executeOrder(Order order, BigDecimal executionPrice) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only pending orders can be executed");
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

    private void reserveBuyFunds(Order order, BigDecimal price) {
        BigDecimal requiredAmount = order.getQuantity().multiply(price);
        Wallet wallet = getLockedActiveWallet(order.getUser().getId());

        if (wallet.getAvailableBalance().compareTo(requiredAmount) < 0) {
            throw new IllegalArgumentException("Insufficient available wallet funds");
        }

        wallet.setReservedBalance(wallet.getReservedBalance().add(requiredAmount));
        saveWalletMovement(wallet, WalletTransactionType.RESERVE, requiredAmount, "BUY order funds reserved");
        order.setReservedAmount(requiredAmount);
    }

    private void releaseBuyFunds(Order order) {
        BigDecimal amount = order.getReservedAmount();
        Wallet wallet = getLockedActiveWallet(order.getUser().getId());

        if (wallet.getReservedBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Reserved wallet funds are insufficient");
        }

        wallet.setReservedBalance(wallet.getReservedBalance().subtract(amount));
        saveWalletMovement(wallet, WalletTransactionType.RELEASE_RESERVED, amount, "BUY order funds released");
        order.setReservedAmount(BigDecimal.ZERO);
    }

    private void executeBuy(Order order, BigDecimal executionPrice, LocalDateTime now) {
        BigDecimal totalAmount = order.getQuantity().multiply(executionPrice);
        Wallet wallet = getLockedActiveWallet(order.getUser().getId());

        if (order.getReservedAmount().compareTo(totalAmount) < 0 || wallet.getReservedBalance().compareTo(totalAmount) < 0) {
            throw new IllegalArgumentException("Reserved wallet funds are insufficient");
        }

        wallet.setReservedBalance(wallet.getReservedBalance().subtract(totalAmount));
        wallet.setBalance(wallet.getBalance().subtract(totalAmount));
        saveWalletMovement(wallet, WalletTransactionType.CAPTURE_RESERVED, totalAmount, "BUY trade executed");

        if (order.getReservedAmount().compareTo(totalAmount) > 0) {
            BigDecimal unusedAmount = order.getReservedAmount().subtract(totalAmount);
            wallet.setReservedBalance(wallet.getReservedBalance().subtract(unusedAmount));
            saveWalletMovement(wallet, WalletTransactionType.RELEASE_RESERVED, unusedAmount, "Unused BUY order funds released");
        }

        Holding holding = holdingRepository.findByUserIdAndAssetIdForUpdate(order.getUser().getId(), order.getAsset().getId())
                .orElse(null);

        if (holding == null) {
            holding = new Holding(order.getUser(), order.getAsset(), order.getQuantity(), executionPrice);
        } else {
            BigDecimal oldQuantity = holding.getQuantity();
            BigDecimal newQuantity = oldQuantity.add(order.getQuantity());
            BigDecimal oldValue = oldQuantity.multiply(holding.getAvgPrice());
            BigDecimal newValue = order.getQuantity().multiply(executionPrice);
            holding.setQuantity(newQuantity);
            holding.setAvgPrice(oldValue.add(newValue).divide(newQuantity, 4, RoundingMode.HALF_UP));
        }

        holdingRepository.save(holding);
        order.setReservedAmount(BigDecimal.ZERO);
        order.setUpdatedAt(now);
    }

    private void reserveSellHolding(Order order) {
        Holding holding = holdingRepository.findByUserIdAndAssetIdForUpdate(order.getUser().getId(), order.getAsset().getId())
                .orElseThrow(() -> new IllegalArgumentException("Holding not found for sell order"));

        if (holding.getAvailableQuantity().compareTo(order.getQuantity()) < 0) {
            throw new IllegalArgumentException("Insufficient available holding quantity");
        }

        holding.setReservedQuantity(holding.getReservedQuantity().add(order.getQuantity()));
        holdingRepository.save(holding);
        order.setReservedQuantity(order.getQuantity());
    }

    private void releaseSellHolding(Order order) {
        Holding holding = holdingRepository.findByUserIdAndAssetIdForUpdate(order.getUser().getId(), order.getAsset().getId())
                .orElseThrow(() -> new IllegalArgumentException("Holding not found for sell order"));

        if (holding.getReservedQuantity().compareTo(order.getReservedQuantity()) < 0) {
            throw new IllegalArgumentException("Reserved holding quantity is insufficient");
        }

        holding.setReservedQuantity(holding.getReservedQuantity().subtract(order.getReservedQuantity()));
        holdingRepository.save(holding);
        order.setReservedQuantity(BigDecimal.ZERO);
    }

    private void executeSell(Order order, BigDecimal executionPrice, LocalDateTime now) {
        Holding holding = holdingRepository.findByUserIdAndAssetIdForUpdate(order.getUser().getId(), order.getAsset().getId())
                .orElseThrow(() -> new IllegalArgumentException("Holding not found for sell order"));

        if (holding.getReservedQuantity().compareTo(order.getReservedQuantity()) < 0 || holding.getQuantity().compareTo(order.getQuantity()) < 0) {
            throw new IllegalArgumentException("Reserved holding quantity is insufficient");
        }

        holding.setReservedQuantity(holding.getReservedQuantity().subtract(order.getReservedQuantity()));
        holding.setQuantity(holding.getQuantity().subtract(order.getQuantity()));
        holdingRepository.save(holding);

        BigDecimal totalAmount = order.getQuantity().multiply(executionPrice);
        Wallet wallet = getLockedActiveWallet(order.getUser().getId());
        wallet.setBalance(wallet.getBalance().add(totalAmount));
        saveWalletMovement(wallet, WalletTransactionType.TRADE_SELL, totalAmount, "SELL trade executed");

        order.setReservedQuantity(BigDecimal.ZERO);
        order.setUpdatedAt(now);
    }

    private Wallet getLockedActiveWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId).orElseThrow(() -> new NoResultException());

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalArgumentException("Wallet is not active");
        }

        return wallet;
    }

    private void saveWalletMovement(Wallet wallet, WalletTransactionType type, BigDecimal amount, String reason) {
        LocalDateTime now = LocalDateTime.now();
        wallet.setUpdatedAt(now);
        walletRepository.save(wallet);
        walletTransactionRepository.save(new WalletTransaction(wallet, type, amount, reason, now));
    }

    private BigDecimal getOrderPrice(Order order) {
        if (order.getOrderCategory() == OrderCategory.MARKET) {
            return order.getAsset().getCurrentPrice();
        }

        return order.getRequestedPrice();
    }

    private void validatePrice(CreateOrderRequest dto) {
        if (dto.getOrderCategory() == OrderCategory.LIMIT) {
            validatePositiveAmount(dto.getRequestedPrice(), "Requested price must be greater than zero for limit orders");
        }
    }

    private void validatePositiveAmount(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateUserActive(User user) {
        if (user.getStatus() != null && user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("User is not active");
        }
    }

    private void validateAssetActive(Asset asset) {
        if (asset.getStatus() != AssetStatus.ACTIVE) {
            throw new IllegalArgumentException("Asset is not active");
        }
    }
}
