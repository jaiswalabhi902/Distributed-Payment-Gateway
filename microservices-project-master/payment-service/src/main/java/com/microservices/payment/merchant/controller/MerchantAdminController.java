package com.microservices.payment.merchant.controller;

import com.microservices.common.constant.Constants;
import com.microservices.common.dto.ApiResponse;
import com.microservices.common.exception.BusinessException;
import com.microservices.payment.merchant.dto.CreateMerchantRequest;
import com.microservices.payment.merchant.dto.MerchantResponse;
import com.microservices.payment.merchant.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Admin API for merchant onboarding. Reached through the gateway (JWT-authenticated);
 * requires ROLE_ADMIN, enforced from the gateway-relayed {@code X-User-Roles} header.
 */
@RestController
@RequestMapping("/api/merchants")
public class MerchantAdminController {

    private final MerchantService merchantService;

    public MerchantAdminController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<MerchantResponse>> create(
            @Valid @RequestBody CreateMerchantRequest request, ServerWebExchange exchange) {
        requireAdmin(exchange);
        return merchantService.create(request)
                .map(m -> ApiResponse.ok("Merchant created — store the key secret now", m));
    }

    @GetMapping
    public Mono<ApiResponse<java.util.List<MerchantResponse>>> list(ServerWebExchange exchange) {
        requireAdmin(exchange);
        return merchantService.list().collectList().map(ApiResponse::ok);
    }

    private void requireAdmin(ServerWebExchange exchange) {
        String roles = exchange.getRequest().getHeaders().getFirst(Constants.Headers.ROLES);
        if (roles == null || !roles.contains(Constants.Roles.ADMIN)) {
            throw new BusinessException("Admin role required");
        }
    }
}
