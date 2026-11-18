
package com.example.inventory_service.service;

import com.example.inventory_service.dto.InventoryReservationRequest;
import com.example.inventory_service.model.Inventory;
import com.example.inventory_service.repository.InventoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }

    public Inventory getInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inventory not found"));
    }

    @Transactional
    public void reserveStock(List<InventoryReservationRequest> requests) {

        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Reservation list cannot be empty");
        }

        Map<Long, Integer> requestedItems = new TreeMap<>();

        // Combine duplicate product IDs before checking available stock.
        for (InventoryReservationRequest request : requests) {

            if (request == null ||
                    request.getProductId() == null ||
                    request.getQuantity() == null ||
                    request.getQuantity() <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid reservation request");
            }

            requestedItems.merge(
                    request.getProductId(),
                    request.getQuantity(),
                    Integer::sum
            );
        }

        List<Inventory> inventories = new ArrayList<>();

        // Lock rows in product ID order to reduce deadlock risk.
        for (Map.Entry<Long, Integer> item : requestedItems.entrySet()) {

            Inventory inventory = inventoryRepository
                    .findByProductIdForUpdate(item.getKey())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Inventory not found"));

            if (inventory.getAvailable() == null ||
                    inventory.getAvailable() < item.getValue()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient stock for product: " + item.getKey());
            }

            inventories.add(inventory);
        }

        // Apply deductions after every stock check passes.
        for (Inventory inventory : inventories) {

            Integer quantity = requestedItems.get(inventory.getProductId());

            inventory.setAvailable(
                    inventory.getAvailable() - quantity
            );
        }

        inventoryRepository.saveAll(inventories);
    }
}
