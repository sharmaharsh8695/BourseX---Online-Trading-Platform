package com.harsh.finance_project.order.controller;

import com.harsh.finance_project.common.dto.PageResponse;
import com.harsh.finance_project.order.dto.CreateOrderRequest;
import com.harsh.finance_project.order.dto.OrderResponse;
import com.harsh.finance_project.order.model.Order;
import com.harsh.finance_project.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest dto) {
        OrderResponse res = orderService.createOrder(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping(value = "/order", params = "id")
    public ResponseEntity<OrderResponse> getOrderById(@RequestParam @NotNull Long id) {
        Order order = orderService.getOrderById(id);

        return ResponseEntity.status(HttpStatus.OK).body(new OrderResponse(order));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getOrderByPathId(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);

        return ResponseEntity.status(HttpStatus.OK).body(new OrderResponse(order));
    }

    @GetMapping("/order")
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(PageResponse.from(orderService.getAllOrders(pageable), OrderResponse::new));
    }

    @GetMapping("/orders/user/{userId}")
    public ResponseEntity<PageResponse<OrderResponse>> getOrdersByUser(@PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(PageResponse.from(orderService.getOrdersByUser(userId, pageable), OrderResponse::new));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        OrderResponse res = orderService.cancelOrder(id);

        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PostMapping("/orders/{id}/execute")
    public ResponseEntity<OrderResponse> executeOrder(@PathVariable Long id) {
        OrderResponse res = orderService.executeOrder(id);

        return ResponseEntity.status(HttpStatus.OK).body(res);
    }
}
