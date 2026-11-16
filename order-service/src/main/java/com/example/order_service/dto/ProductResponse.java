package com.example.order_service.dto;

/**
 * What product-service sends back from GET /api/products/{id}.
 *
 * This service never reads the products table. It asks product-service and
 * Jackson turns the JSON into this object. Only price is actually used, but
 * the other fields are listed so the mapping is obvious.
 */
public class ProductResponse {

    private Long id;
    private String name;
    private Double price;
    private Integer quantity;
    private String imageUrl;

    public ProductResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
