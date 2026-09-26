package com.harsh.finance_project.holding.controller;

import com.harsh.finance_project.common.dto.PageResponse;
import com.harsh.finance_project.holding.dto.HoldingResponse;
import com.harsh.finance_project.holding.model.Holding;
import com.harsh.finance_project.holding.service.HoldingService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HoldingController {
    private final HoldingService holdingService;

    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @PostMapping("/holding")
    public void createHolding() {
        throw new AccessDeniedException("Holdings are created only by trade execution");
    }

    @GetMapping(value = "/holding", params = "id")
    public ResponseEntity<HoldingResponse> getHoldingById(@RequestParam @NotNull Long id) {
        Holding holding = holdingService.getHoldingById(id);

        return ResponseEntity.status(HttpStatus.OK).body(new HoldingResponse(holding));
    }

    @GetMapping("/holdings/{id}")
    public ResponseEntity<HoldingResponse> getHoldingByPathId(@PathVariable Long id) {
        Holding holding = holdingService.getHoldingById(id);

        return ResponseEntity.status(HttpStatus.OK).body(new HoldingResponse(holding));
    }

    @GetMapping("/holding")
    public ResponseEntity<PageResponse<HoldingResponse>> getAllHoldings(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(PageResponse.from(holdingService.getAllHoldings(pageable), HoldingResponse::new));
    }

    @GetMapping("/holdings/user/{userId}")
    public ResponseEntity<PageResponse<HoldingResponse>> getHoldingsByUser(@PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(PageResponse.from(holdingService.getHoldingsByUser(userId, pageable), HoldingResponse::new));
    }

    @PutMapping("/holding")
    public void updateHolding() {
        throw new AccessDeniedException("Holdings are updated only by trade execution");
    }

    @DeleteMapping("/holdings/{id}")
    public void deleteHolding(@PathVariable Long id) {
        throw new AccessDeniedException("Holdings cannot be deleted through the API");
    }

}
