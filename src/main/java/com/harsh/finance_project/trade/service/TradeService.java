package com.harsh.finance_project.trade.service;

import com.harsh.finance_project.trade.model.Trade;
import com.harsh.finance_project.trade.repository.TradeRepository;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TradeService {
    private final TradeRepository tradeRepository;

    public TradeService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    public Trade getTradeById(Long id) {
        return tradeRepository.findById(id).orElseThrow(() -> new NoResultException());
    }

    public List<Trade> getAllTrades() {
        return tradeRepository.findAll();
    }

    public List<Trade> getTradesByUser(Long userId) {
        return tradeRepository.findByUserId(userId);
    }

    public List<Trade> getTradesByOrder(Long orderId) {
        return tradeRepository.findByOrderId(orderId);
    }
}
