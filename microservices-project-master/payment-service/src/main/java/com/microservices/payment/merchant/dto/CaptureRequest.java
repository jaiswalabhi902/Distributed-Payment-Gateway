package com.microservices.payment.merchant.dto;

import com.microservices.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Simulates what the checkout/PSP would submit. With a real PSP this is replaced by
 * the provider's tokenized payment instrument; here we accept the method (+ optional
 * test hints) for the mock provider.
 */
@Data
public class CaptureRequest {

    @NotNull(message = "method is required")
    private PaymentMethod method;

    /** Optional UPI VPA for UPI payments (e.g. success@upi / failure@upi for mock). */
    private String vpa;

    /** Optional flag to force a failed charge in the mock provider. */
    private boolean simulateFailure;
}
