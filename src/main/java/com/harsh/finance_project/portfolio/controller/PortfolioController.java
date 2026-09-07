package com.harsh.finance_project.portfolio.controller;

import com.harsh.finance_project.common.dto.PageResponse;
import com.harsh.finance_project.portfolio.dto.HoldingSummaryResponse;
import com.harsh.finance_project.portfolio.dto.PortfolioResponse;
import com.harsh.finance_project.portfolio.service.PortfolioService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PortfolioController {
    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/portfolio/{userId}")
    public ResponseEntity<PortfolioResponse> getPortfolio(@PathVariable Long userId) {
        PortfolioResponse res = portfolioService.getPortfolio(userId);

        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @GetMapping("/portfolio/{userId}/holdings")
    public ResponseEntity<PageResponse<HoldingSummaryResponse>> getPortfolioHoldings(@PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new PageResponse<>(portfolioService.getPortfolioHoldings(userId, pageable)));
    }
}
