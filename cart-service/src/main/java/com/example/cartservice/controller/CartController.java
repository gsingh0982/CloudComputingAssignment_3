
package com.example.cartservice.controller;

import com.example.cartservice.dto.AddItemRequest;
import com.example.cartservice.dto.CartResponse;
import com.example.cartservice.dto.OrderResponse;
import com.example.cartservice.service.CartService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
@CrossOrigin(origins = "http://localhost:3000")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<CartResponse> createCart() {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.createCart());
    }

    @GetMapping("/{id}")
    public CartResponse getCart(@PathVariable Long id) {

        return cartService.getCart(id);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable Long id,
            @RequestBody AddItemRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItem(id, request));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            @PathVariable Long id,
            @PathVariable Long itemId) {

        cartService.removeItem(id, itemId);
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<List<OrderResponse>> checkout(
            @PathVariable Long id) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.checkout(id));
    }
}
