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

/** Coordinates product lookup, inventory reservation, and order persistence. */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestClient restClient;

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
     * Reserves inventory before persisting orders; insufficient stock returns 409.
     * A persistence failure does not roll back the remote inventory reservation.
     */
    public List<Order> createOrders(List<OrderLineRequest> lines) {

        validate(lines);

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

        reserveStock(lines);

        return orderRepository.saveAll(toSave);
    }

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

    private ProductResponse fetchProduct(Long productId) {
        try {
            return restClient.get()
                    .uri(productServiceUrl + "/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductResponse.class);

        } catch (HttpClientErrorException.NotFound e) {
            // A missing product makes the checkout request invalid.
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "No product with id: " + productId);
        }
    }

    private void reserveStock(List<OrderLineRequest> lines) {
        try {
            restClient.post()
                    .uri(inventoryServiceUrl + "/api/inventory/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(lines)
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException.Conflict e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Not enough stock for one or more items");

        } catch (HttpClientErrorException e) {
            // Treat other inventory 4xx responses as invalid checkout requests.
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Inventory rejected the order: " + e.getStatusText());
        }
    }
}
