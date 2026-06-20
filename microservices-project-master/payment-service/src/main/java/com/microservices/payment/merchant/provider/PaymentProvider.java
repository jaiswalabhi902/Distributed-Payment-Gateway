package com.microservices.payment.merchant.provider;

import com.microservices.payment.domain.PaymentMethod;
import reactor.core.publisher.Mono;

/**
 * Abstraction over the upstream PSP / acquirer that actually moves money
 * (UPI / cards / netbanking). Swap {@link MockPaymentProvider} for a Razorpay or
 * Cashfree implementation without touching the order/merchant logic.
 */
public interface PaymentProvider {

    Mono<ProviderResult> charge(ProviderCharge charge);

    record ProviderCharge(
            String orderRef,
            long amount,
            String currency,
            PaymentMethod method,
            String vpa,
            boolean simulateFailure) {
    }

    record ProviderResult(boolean success, String providerReference, String failureReason) {
        public static ProviderResult ok(String ref) {
            return new ProviderResult(true, ref, null);
        }

        public static ProviderResult failed(String reason) {
            return new ProviderResult(false, null, reason);
        }
    }
}
