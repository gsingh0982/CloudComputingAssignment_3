package com.example.order_service.dto;

/** Checkout line shared by the order and inventory reservation APIs. */
public class OrderLineRequest {

    private Long productId;
    private Integer quantity;

    public OrderLineRequest() {
    }

    public OrderLineRequest(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
