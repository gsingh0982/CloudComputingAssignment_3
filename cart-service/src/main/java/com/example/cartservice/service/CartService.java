
package com.example.cartservice.service;

import com.example.cartservice.dto.AddItemRequest;
import com.example.cartservice.dto.CartResponse;
import com.example.cartservice.dto.OrderItem;
import com.example.cartservice.dto.OrderResponse;

import com.example.cartservice.model.Cart;
import com.example.cartservice.model.CartItem;
import com.example.cartservice.repository.CartRepository;
import com.example.cartservice.repository.CartItemRepository;

import jakarta.transaction.Transactional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository itemRepository;
    private final RestClient orderRestClient;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository itemRepository,
            RestClient orderRestClient) {

        this.cartRepository = cartRepository;
        this.itemRepository = itemRepository;
        this.orderRestClient = orderRestClient;
    }

    @Transactional
    public CartResponse createCart() {

        Cart cart = new Cart();
        cart.setStatus(Cart.Status.OPEN);

        cartRepository.save(cart);

        return getCart(cart.getId());
    }

    public CartResponse getCart(Long cartId) {

        Cart cart = findCart(cartId);

        List<CartItem> items = itemRepository.findByCartId(cartId);

        return new CartResponse(
                cart.getId(),
                cart.getStatus(),
                items
        );
    }

    @Transactional
    public CartResponse addItem(
            Long cartId,
            AddItemRequest request) {

        Cart cart = findCart(cartId);
        requireOpen(cart);

        if (request == null ||
                request.getProductId() == null ||
                request.getProductId() <= 0 ||
                request.getQuantity() == null ||
                request.getQuantity() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid product or quantity"
            );
        }

        CartItem item = new CartItem();

        item.setCartId(cartId);
        item.setProductId(request.getProductId());
        item.setQuantity(request.getQuantity());

        itemRepository.saveAndFlush(item);

        return getCart(cartId);
    }

    @Transactional
    public void removeItem(Long cartId, Long itemId) {

        Cart cart = findCart(cartId);
        requireOpen(cart);

        CartItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item not found"
                ));

        // Reject item IDs belonging to another cart.
        if (!item.getCartId().equals(cartId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Item does not belong to this cart"
            );
        }

        itemRepository.delete(item);
    }

    public List<OrderResponse> checkout(Long cartId) {

        Cart cart = findCart(cartId);
        requireOpen(cart);

        List<CartItem> items = itemRepository.findByCartId(cartId);

        if (items.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cart is empty"
            );
        }

        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem item : items) {
            orderItems.add(new OrderItem(
                    item.getProductId(),
                    item.getQuantity()
            ));
        }

        List<OrderResponse> orders;

        try {

            orders = orderRestClient.post()
                    .uri("/api/orders")
                    .body(orderItems)
                    .retrieve()
                    .body(new ParameterizedTypeReference<
                            List<OrderResponse>>() {});

        } catch (RestClientResponseException ex) {

            // Preserve checkout validation and stock errors from order-service.
            if (ex.getStatusCode().value() == 409 ||
                    ex.getStatusCode().value() == 400) {

                throw new ResponseStatusException(
                        ex.getStatusCode(),
                        "Order Service rejected checkout",
                        ex
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Order Service error",
                    ex
            );

        } catch (ResourceAccessException ex) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Order Service unavailable",
                    ex
            );
        }

        // Expect one order per cart line before marking the cart checked out.
        if (orders == null || orders.size() != items.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unexpected response from Order Service"
            );
        }

        // Mark the cart checked out only after order-service succeeds.
        cart.setStatus(Cart.Status.CHECKED_OUT);
        cartRepository.save(cart);

        return orders;
    }

    private Cart findCart(Long cartId) {

        return cartRepository.findById(cartId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cart not found"
                ));
    }

    private void requireOpen(Cart cart) {

        if (cart.getStatus() != Cart.Status.OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cart is already checked out"
            );
        }
    }
}
