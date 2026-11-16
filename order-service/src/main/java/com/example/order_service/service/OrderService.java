package com.example.order_service.service;

import com.example.order_service.dto.OrderLineRequest;
import com.example.order_service.dto.ProductResponse;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * The checkout orchestrator.
 *
 * This is the only service that calls two others. For each line it asks
 * product-service for the current price, then asks inventory-service to
 * reserve the whole basket at once, and only saves orders if that succeeded.
 *
 * It never touches the products or inventory tables. If it needs a price it
 * asks the service that owns it.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestClient restClient;

    // Addresses come from application.properties, overridden by environment
    // variables in docker-compose. Never hard-coded.
    private final String productServiceUrl;
    private final String inventoryServiceUrl;

    public OrderService(OrderRepository orderRepository,
                        RestClient restClient,
                        @Value("${product.service.url}") String productServiceUrl,
                        @Value("${inventory.service.url}") String inventoryServiceUrl) {
        this.orderRepository = orderRepository;
        this.restClient = restClient;
        this.productServiceUrl = productServiceUrl;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Steps 3 to 5 of the checkout flow.
     *
     * Stock is reserved BEFORE anything is saved, so a basket that is short
     * fails with 409 and leaves the orders table untouched.
     */
    public List<Order> createOrders(List<OrderLineRequest> lines) {

        validate(lines);

        // Step 3 - ask product-service to confirm each product and give its price.
        List<Order> toSave = new ArrayList<>();

        for (OrderLineRequest line : lines) {
            ProductResponse product = fetchProduct(line.getProductId());

            double totalPrice = product.getPrice() * line.getQuantity();

            toSave.add(new Order(
                    line.getProductId(),
                    line.getQuantity(),
                    totalPrice,
                    "CONFIRMED"));
        }

        // Step 4 - reserve the whole basket. Throws 409 if any line is short,
        // and inventory-service reserves nothing in that case.
        reserveStock(lines);

        // Step 5 - stock is held, so the orders can be saved.
        return orderRepository.saveAll(toSave);
    }

    /** A malformed basket is the client's mistake, so 400. */
    private void validate(List<OrderLineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Order must contain at least one line");
        }

        for (OrderLineRequest line : lines) {
            if (line.getProductId() == null
                    || line.getQuantity() == null
                    || line.getQuantity() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Each line needs a productId and a quantity above 0");
            }
        }
    }

    /** GET {product-service}/api/products/{id} */
    private ProductResponse fetchProduct(Long productId) {
        try {
            return restClient.get()
                    .uri(productServiceUrl + "/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductResponse.class);

        } catch (HttpClientErrorException.NotFound e) {
            // The product does not exist, so the basket itself is wrong: 400.
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "No product with id: " + productId);
        }
    }

    /** POST {inventory-service}/api/inventory/reserve */
    private void reserveStock(List<OrderLineRequest> lines) {
        try {
            restClient.post()
                    .uri(inventoryServiceUrl + "/api/inventory/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(lines)
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException.Conflict e) {
            // Not enough stock. Nothing was reserved and nothing is saved.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Not enough stock for one or more items");

        } catch (HttpClientErrorException e) {
            // Any other refusal from inventory-service (for example a product
            // with no inventory row) is still a bad basket.
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Inventory rejected the order: " + e.getStatusText());
        }
    }
}
