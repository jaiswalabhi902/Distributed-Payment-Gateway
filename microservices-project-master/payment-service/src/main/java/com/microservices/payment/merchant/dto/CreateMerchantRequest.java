package com.microservices.payment.merchant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMerchantRequest {

    @NotBlank(message = "Merchant name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /** Optional server-to-server webhook URL for payment notifications. */
    private String webhookUrl;
}
