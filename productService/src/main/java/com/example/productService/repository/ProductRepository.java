package com.example.productService.repository;

import com.example.productService.model.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity,Long> {
    // Derived queries
    List<ProductEntity> findByNameContainingIgnoreCase(String name);

    List<ProductEntity> findByPriceBetween(Double min, Double max);

    // Custom query
    @Query("SELECT p FROM ProductEntity p WHERE p.quantity < :limit")
    List<ProductEntity> findLowStock(@Param("limit") Integer limit);
}

