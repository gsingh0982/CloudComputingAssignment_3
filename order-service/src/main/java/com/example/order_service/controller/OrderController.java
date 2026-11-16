package com.example.order_service.controller;

import com.example.order_service.dto.OrderLineRequest;
import com.example.order_service.model.Order;
import com.example.order_service.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

/**
 * HTTP layer. Turns requests into service calls and results into status codes.
 * No business logic lives here.
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** GET /api/orders -> 200 + every order */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /**
     * POST /api/orders -> 201 + the orders created.
     * 400 if the basket is malformed, 409 if stock is short.
     */
    @PostMapping
    public ResponseEntity<List<Order>> createOrders(@RequestBody List<OrderLineRequest> lines) {
        List<Order> created = orderService.createOrders(lines);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
