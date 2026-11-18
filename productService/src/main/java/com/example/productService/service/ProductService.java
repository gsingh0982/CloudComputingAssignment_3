package com.example.productService.service;

import com.example.productService.model.ProductEntity;
import com.example.productService.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<ProductEntity> getAllProducts() {
        return repository.findAll();
    }

    public ProductEntity getProductById(Long id) {
        Optional<ProductEntity> found = repository.findById(id);
        if (found.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
        }
        return found.get();
    }

    public ProductEntity createProduct(ProductEntity product) {
        validateProduct(product);
        product.setId(null);
        return repository.save(product);
    }

    public ProductEntity updateProduct(Long id, ProductEntity updated) {
        ProductEntity current = getProductById(id);
        validateProduct(updated);
        current.setName(updated.getName());
        current.setPrice(updated.getPrice());
        current.setQuantity(updated.getQuantity());
        // keep the old image if the request does not send one
        if (updated.getImageUrl() != null) {
            current.setImageUrl(updated.getImageUrl());
        }
        return repository.save(current);
    }

    public void deleteProduct(Long id) {
        ProductEntity product = getProductById(id);
        repository.delete(product);
    }

    // Searches
    public List<ProductEntity> searchByName(String name) {
        return repository.findByNameContainingIgnoreCase(name);
    }

    public List<ProductEntity> searchByPriceRange(Double min, Double max) {
        return repository.findByPriceBetween(min, max);
    }

    public List<ProductEntity> findLowStock(Integer limit) {
        return repository.findLowStock(limit);
    }

    // Validation
    private void validateProduct(ProductEntity product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        if (product.getPrice() == null || product.getPrice() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price is required and cannot be negative");
        }
        if (product.getQuantity() == null || product.getQuantity() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity is required and cannot be negative");
        }
    }
}