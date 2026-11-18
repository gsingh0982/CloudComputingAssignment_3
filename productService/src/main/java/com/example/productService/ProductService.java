package com.example.productService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<ProductEntity> getAll() {
        return repository.findAll();
    }

    public ProductEntity getById(long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public List<ProductEntity> searchByName(String name) {
        return repository.findByNameContainingIgnoreCase(name);
    }

    public List<ProductEntity> searchByPrice(double min, double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid price range");
        }
        return repository.findByPriceBetween(min, max);
    }

    public ProductEntity create(ProductWriteRequest request) {
        ProductEntity product = new ProductEntity();
        applyDetails(product, request);
        return repository.save(product);
    }

    @Transactional
    public ProductEntity update(long id, ProductWriteRequest request) {
        ProductEntity product = getById(id);
        applyDetails(product, request);
        return product;
    }

    @Transactional
    public void delete(long id) {
        repository.delete(getById(id));
    }

    private void applyDetails(ProductEntity product, ProductWriteRequest request) {
        if (!Double.isFinite(request.price())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be finite");
        }
        product.setName(request.name());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
        // Preserve the thumbnail when an Assignment Two request omits imageUrl.
        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
        }
    }
}
