package com.example.inventory_service.dto;


public class InventoryReservationRequest {

    private Long productId;
    private Integer quantity;

    public InventoryReservationRequest() {
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
