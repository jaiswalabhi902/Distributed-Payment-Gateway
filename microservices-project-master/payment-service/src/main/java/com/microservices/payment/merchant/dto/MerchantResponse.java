package com.microservices.payment.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantResponse {

    private Long id;
    private String name;
    private String email;
    private String keyId;

    /**
     * The raw key secret. Returned ONLY in the response that creates the merchant —
     * it is never retrievable again (stored encrypted, shown once).
     */
    private String keySecret;

    private String webhookUrl;
    private boolean active;
    private Instant createdAt;
}
