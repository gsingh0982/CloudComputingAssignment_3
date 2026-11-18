package com.example.productService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductServiceController {
    private final ProductService service;

    public ProductServiceController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductEntity> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ProductEntity getById(@PathVariable long id) {
        return service.getById(id);
    }

    @GetMapping("/search")
    public List<ProductEntity> searchByName(@RequestParam String name) {
        return service.searchByName(name);
    }

    @GetMapping("/search/price")
    public List<ProductEntity> searchByPrice(@RequestParam double min, @RequestParam double max) {
        return service.searchByPrice(min, max);
    }

    @PostMapping
    public ResponseEntity<ProductEntity> create(@Valid @RequestBody ProductWriteRequest request) {
        ProductEntity product = service.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + product.getId())).body(product);
    }

    @PutMapping("/{id}")
    public ProductEntity update(@PathVariable long id, @Valid @RequestBody ProductWriteRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
