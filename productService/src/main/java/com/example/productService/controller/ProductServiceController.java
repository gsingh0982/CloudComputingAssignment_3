package com.example.productService.controller;

  import com.example.productService.model.ProductEntity;
  import com.example.productService.service.ProductService;
  import org.springframework.http.HttpStatus;
  import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductServiceController {

    private final ProductService service;

    public ProductServiceController(ProductService service) {
        this.service = service;
    }

    // CRUD
    @GetMapping
    public List<ProductEntity> getAll() {
        return service.getAllProducts();
    }

    @GetMapping("/{id}")
    public ProductEntity getById(@PathVariable Long id) {
        return service.getProductById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductEntity create(@RequestBody ProductEntity product) {
        return service.createProduct(product);
    }

    @PutMapping("/{id}")
    public ProductEntity update(@PathVariable Long id, @RequestBody ProductEntity product) {
        return service.updateProduct(id, product);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteProduct(id);
    }

    // Search
    @GetMapping("/search")
    public List<ProductEntity> searchByName(@RequestParam String name) {
        return service.searchByName(name);
    }

    @GetMapping("/search/price")
    public List<ProductEntity> searchByPriceRange(@RequestParam Double min, @RequestParam Double max) {
        return service.searchByPriceRange(min, max);
    }

    @GetMapping("/low-stock")
    public List<ProductEntity> lowStock(@RequestParam Integer limit) {
        return service.findLowStock(limit);
    }
}
