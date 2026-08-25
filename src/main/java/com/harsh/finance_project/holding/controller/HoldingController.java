package com.harsh.finance_project.holding.controller;

import com.harsh.finance_project.holding.dto.CreateHoldingRequest;
import com.harsh.finance_project.holding.dto.HoldingResponse;
import com.harsh.finance_project.holding.dto.UpdateHoldingRequest;
import com.harsh.finance_project.holding.model.Holding;
import com.harsh.finance_project.holding.service.HoldingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HoldingController {
    private final HoldingService holdingService;

    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @PostMapping("/holding")
    public ResponseEntity<HoldingResponse> createHolding(@Valid @RequestBody CreateHoldingRequest dto) {
        HoldingResponse res = holdingService.createHolding(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(res);
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
    public ResponseEntity<List<HoldingResponse>> getAllHoldings() {
        List<Holding> holdings = holdingService.getAllHoldings();

        return ResponseEntity.status(HttpStatus.OK).body(holdings.stream()
                .map(holding -> new HoldingResponse(holding))
                .toList());
    }

    @GetMapping("/holdings/user/{userId}")
    public ResponseEntity<List<HoldingResponse>> getHoldingsByUser(@PathVariable Long userId) {
        List<Holding> holdings = holdingService.getHoldingsByUser(userId);

        return ResponseEntity.status(HttpStatus.OK).body(holdings.stream()
                .map(holding -> new HoldingResponse(holding))
                .toList());
    }

    @PutMapping("/holding")
    public ResponseEntity<HoldingResponse> updateHolding(@RequestParam @NotNull Long id, @RequestBody @Valid UpdateHoldingRequest dto) {
        Holding holding = holdingService.updateHolding(id, dto);

        return ResponseEntity.status(HttpStatus.OK).body(new HoldingResponse(holding));
    }

    @DeleteMapping("/holdings/{id}")
    public ResponseEntity<String> deleteHolding(@PathVariable Long id) {
        holdingService.deleteHolding(id);

        return ResponseEntity.status(HttpStatus.OK).body("Holding Deleted Successfully");
    }
}
