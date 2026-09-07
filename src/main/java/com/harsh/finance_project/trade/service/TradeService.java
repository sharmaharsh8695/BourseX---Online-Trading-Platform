package com.harsh.finance_project.trade.service;

import com.harsh.finance_project.common.web.PageableUtil;
import com.harsh.finance_project.trade.model.Trade;
import com.harsh.finance_project.trade.repository.TradeRepository;
import jakarta.persistence.NoResultException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TradeService {
    private final TradeRepository tradeRepository;

    public TradeService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    public Trade getTradeById(Long id) {
        return tradeRepository.findWithOrderUserAssetById(id).orElseThrow(() -> new NoResultException());
    }

    public Page<Trade> getAllTrades(Pageable pageable) {
        return tradeRepository.findAll(PageableUtil.bounded(pageable));
    }

    public Page<Trade> getTradesByUser(Long userId, Pageable pageable) {
        return tradeRepository.findByUserId(userId, PageableUtil.bounded(pageable));
    }

    public Page<Trade> getTradesByOrder(Long orderId, Pageable pageable) {
        return tradeRepository.findByOrderId(orderId, PageableUtil.bounded(pageable));
    }
}
