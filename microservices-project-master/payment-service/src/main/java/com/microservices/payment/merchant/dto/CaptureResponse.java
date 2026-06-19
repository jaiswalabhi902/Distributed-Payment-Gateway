package com.microservices.payment.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned to the checkout after a charge. The merchant's server verifies
 * {@code signature == HMAC_SHA256(orderId + "|" + paymentId, key_secret)}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptureResponse {

    private String paymentId;
    private String orderId;
    private String signature;
    private String status;
    private Long amount;
    private String currency;
}
