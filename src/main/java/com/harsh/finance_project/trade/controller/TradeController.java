package com.harsh.finance_project.trade.controller;

import com.harsh.finance_project.common.dto.PageResponse;
import com.harsh.finance_project.trade.dto.TradeResponse;
import com.harsh.finance_project.trade.model.Trade;
import com.harsh.finance_project.trade.service.TradeService;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<PageResponse<TradeResponse>> getAllTrades(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(PageResponse.from(tradeService.getAllTrades(pageable), TradeResponse::new));
    }

    @GetMapping("/trades/user/{userId}")
    public ResponseEntity<PageResponse<TradeResponse>> getTradesByUser(@PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(PageResponse.from(tradeService.getTradesByUser(userId, pageable), TradeResponse::new));
    }

    @GetMapping("/trades/order/{orderId}")
    public ResponseEntity<PageResponse<TradeResponse>> getTradesByOrder(@PathVariable Long orderId, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(PageResponse.from(tradeService.getTradesByOrder(orderId, pageable), TradeResponse::new));
    }
}
