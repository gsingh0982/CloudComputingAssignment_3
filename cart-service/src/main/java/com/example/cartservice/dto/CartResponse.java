package com.example.cartservice.dto;

import com.example.cartservice.model.Cart;
import com.example.cartservice.model.CartItem;

import java.util.List;

public class CartResponse {
    private Long id;
    private Cart.Status status;
    private List<CartItem> items;

    public CartResponse() {
    }

    public CartResponse(Long id, Cart.Status status,
                        List<CartItem> items) {
        this.id = id;
        this.status = status;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cart.Status getStatus() {
        return status;
    }

    public void setStatus(Cart.Status status) {
        this.status = status;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }
}
