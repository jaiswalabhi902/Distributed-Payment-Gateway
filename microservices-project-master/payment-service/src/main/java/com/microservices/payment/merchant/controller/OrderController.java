package com.microservices.payment.merchant.controller;

import com.microservices.payment.merchant.dto.CaptureRequest;
import com.microservices.payment.merchant.dto.CaptureResponse;
import com.microservices.payment.merchant.dto.CreateOrderRequest;
import com.microservices.payment.merchant.dto.OrderResponse;
import com.microservices.payment.merchant.dto.VerifyRequest;
import com.microservices.payment.merchant.entity.Merchant;
import com.microservices.payment.merchant.security.MerchantApiKeyFilter;
import com.microservices.payment.merchant.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Public merchant API (authenticated by API key via {@link MerchantApiKeyFilter}).
 * This is what a merchant's website integrates against.
 */
@RestController
@RequestMapping("/v1")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request, ServerWebExchange exchange) {
        return orderService.createOrder(merchant(exchange), request);
    }

    @GetMapping("/orders")
    public Flux<OrderResponse> listOrders(ServerWebExchange exchange) {
        return orderService.listOrders(merchant(exchange));
    }

    @GetMapping("/orders/{orderRef}")
    public Mono<OrderResponse> getOrder(
            @PathVariable String orderRef, ServerWebExchange exchange) {
        return orderService.getOrder(merchant(exchange), orderRef);
    }

    /**
     * Captures a payment for an order. In production the checkout SDK + PSP do this;
     * here the mock provider simulates the UPI/card charge.
     */
    @PostMapping("/orders/{orderRef}/pay")
    public Mono<CaptureResponse> pay(
            @PathVariable String orderRef,
            @Valid @RequestBody CaptureRequest request,
            ServerWebExchange exchange) {
        return orderService.capture(merchant(exchange), orderRef, request);
    }

    @PostMapping("/payments/verify")
    public Mono<Map<String, Boolean>> verify(
            @Valid @RequestBody VerifyRequest request, ServerWebExchange exchange) {
        return orderService.verify(merchant(exchange), request)
                .map(valid -> Map.of("valid", valid));
    }

    private Merchant merchant(ServerWebExchange exchange) {
        return exchange.getAttribute(MerchantApiKeyFilter.MERCHANT_ATTR);
    }
}
