package com.harsh.finance_project.trade.controller;

import com.harsh.finance_project.trade.dto.TradeResponse;
import com.harsh.finance_project.trade.model.Trade;
import com.harsh.finance_project.trade.service.TradeService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TradeController {
    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping(value = "/trade", params = "id")
    public ResponseEntity<TradeResponse> getTradeById(@RequestParam @NotNull Long id) {
        Trade trade = tradeService.getTradeById(id);

        return ResponseEntity.status(HttpStatus.OK).body(new TradeResponse(trade));
    }

    @GetMapping("/trades/{id}")
    public ResponseEntity<TradeResponse> getTradeByPathId(@PathVariable Long id) {
        Trade trade = tradeService.getTradeById(id);

        return ResponseEntity.status(HttpStatus.OK).body(new TradeResponse(trade));
    }

    @GetMapping("/trade")
    public ResponseEntity<List<TradeResponse>> getAllTrades() {
        List<Trade> trades = tradeService.getAllTrades();

        return ResponseEntity.status(HttpStatus.OK).body(trades.stream()
                .map(trade -> new TradeResponse(trade))
                .toList());
    }

    @GetMapping("/trades/user/{userId}")
    public ResponseEntity<List<TradeResponse>> getTradesByUser(@PathVariable Long userId) {
        List<Trade> trades = tradeService.getTradesByUser(userId);

        return ResponseEntity.status(HttpStatus.OK).body(trades.stream()
                .map(trade -> new TradeResponse(trade))
                .toList());
    }

    @GetMapping("/trades/order/{orderId}")
    public ResponseEntity<List<TradeResponse>> getTradesByOrder(@PathVariable Long orderId) {
        List<Trade> trades = tradeService.getTradesByOrder(orderId);

        return ResponseEntity.status(HttpStatus.OK).body(trades.stream()
                .map(trade -> new TradeResponse(trade))
                .toList());
    }
}
