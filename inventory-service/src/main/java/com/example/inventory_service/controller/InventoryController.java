

package com.example.inventory_service.controller;

import com.example.inventory_service.dto.InventoryReservationRequest;
import com.example.inventory_service.model.Inventory;
import com.example.inventory_service.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:3000")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<Inventory> getAllInventory() {
        return inventoryService.getAllInventory();
    }

    @GetMapping("/{productId}")
    public Inventory getInventoryByProductId(@PathVariable Long productId) {
        return inventoryService.getInventoryByProductId(productId);
    }

    @PostMapping("/reserve")
    public ResponseEntity<String> reserveStock(
            @RequestBody List<InventoryReservationRequest> requests) {

        inventoryService.reserveStock(requests);

        return ResponseEntity.ok("Stock reserved successfully");
    }
}
