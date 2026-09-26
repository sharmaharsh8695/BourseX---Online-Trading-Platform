package com.harsh.finance_project.trade.service;

import com.harsh.finance_project.common.web.PageableUtil;
import com.harsh.finance_project.trade.model.Trade;
import com.harsh.finance_project.trade.repository.TradeRepository;
import com.harsh.finance_project.order.model.Order;
import com.harsh.finance_project.order.repository.OrderRepository;
import com.harsh.finance_project.exception.OrderNotFoundException;
import com.harsh.finance_project.security.ResourceOwnershipService;
import jakarta.persistence.NoResultException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class TradeService {
    private final TradeRepository tradeRepository;
    private final OrderRepository orderRepository;
    private final ResourceOwnershipService ownershipService;

    public TradeService(TradeRepository tradeRepository, OrderRepository orderRepository, ResourceOwnershipService ownershipService) {
        this.tradeRepository = tradeRepository;
        this.orderRepository = orderRepository;
        this.ownershipService = ownershipService;
    }

    public Trade getTradeById(Long id) {
        Trade trade = tradeRepository.findWithOrderUserAssetById(id).orElseThrow(() -> new NoResultException());
        ownershipService.requireOwner(trade.getUser().getId());
        return trade;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<Trade> getAllTrades(Pageable pageable) {
        return tradeRepository.findAll(PageableUtil.bounded(pageable));
    }

    public Page<Trade> getTradesByUser(Long userId, Pageable pageable) {
        ownershipService.requireOwner(userId);
        return tradeRepository.findByUserId(userId, PageableUtil.bounded(pageable));
    }

    public Page<Trade> getTradesByOrder(Long orderId, Pageable pageable) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        ownershipService.requireOwner(order.getUser().getId());
        return tradeRepository.findByOrderId(orderId, PageableUtil.bounded(pageable));
    }
}
