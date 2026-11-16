package com.example.order_service.repository;

import com.example.order_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for orders. Spring generates the implementation at startup;
 * findAll() and save() are inherited from JpaRepository.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
}
