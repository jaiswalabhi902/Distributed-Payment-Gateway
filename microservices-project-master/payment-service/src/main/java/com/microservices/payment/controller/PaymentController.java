package com.microservices.payment.controller;

import com.microservices.common.constant.Constants;
import com.microservices.common.dto.ApiResponse;
import com.microservices.payment.domain.PaymentStatus;
import com.microservices.payment.dto.CreatePaymentRequest;
import com.microservices.payment.dto.PaymentResponse;
import com.microservices.payment.dto.RefundRequest;
import com.microservices.payment.dto.UpdateStatusRequest;
import com.microservices.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<PaymentResponse>> create(
            @RequestHeader(Constants.Headers.USER_ID) Long userId,
            @Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(userId, request)
                .map(p -> ApiResponse.ok("Payment created", p));
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<PaymentResponse>> get(@PathVariable Long id) {
        return paymentService.getPayment(id).map(ApiResponse::ok);
    }

    @GetMapping("/user/{userId}")
    public Mono<ApiResponse<List<PaymentResponse>>> byUser(@PathVariable Long userId) {
        return paymentService.getUserPayments(userId).collectList().map(ApiResponse::ok);
    }

    @GetMapping("/status/{status}")
    public Mono<ApiResponse<List<PaymentResponse>>> byStatus(@PathVariable PaymentStatus status) {
        return paymentService.getByStatus(status).collectList().map(ApiResponse::ok);
    }

    @GetMapping("/user/{userId}/count")
    public Mono<ApiResponse<Long>> count(@PathVariable Long userId) {
        return paymentService.countUserPayments(userId).map(ApiResponse::ok);
    }

    @PutMapping("/{id}/status")
    public Mono<ApiResponse<PaymentResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return paymentService.updateStatus(id, request.getStatus(), request.getReason())
                .map(p -> ApiResponse.ok("Status updated", p));
    }

    @PostMapping("/{id}/refund")
    public Mono<ApiResponse<PaymentResponse>> refund(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request) {
        return paymentService.refund(id, request)
                .map(p -> ApiResponse.ok("Refund processed", p));
    }

    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "UP", "service", "payment-service"));
    }
}
