package com.example.order_service.dto;

/**
 * One line of a checkout: "2 of product 5".
 *
 * Used twice: it is the shape cart-service POSTs to /api/orders, and the
 * shape this service POSTs on to inventory-service /api/inventory/reserve.
 * Both endpoints take a list of {productId, quantity}, so one class covers both.
 */
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
