package com.microservices.payment.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyRequest {

    @NotBlank(message = "orderId is required")
    private String orderId;

    @NotBlank(message = "paymentId is required")
    private String paymentId;

    @NotBlank(message = "signature is required")
    private String signature;
}
