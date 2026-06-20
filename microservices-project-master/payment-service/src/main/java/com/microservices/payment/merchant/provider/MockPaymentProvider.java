package com.microservices.payment.merchant.provider;

import com.microservices.payment.merchant.crypto.CryptoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Sandbox provider that simulates a UPI/card charge so the full flow runs without a
 * real PSP. Fails the charge when {@code simulateFailure} is set or the UPI VPA
 * contains "failure" — mirroring how PSP test credentials trigger failures.
 */
@Slf4j
@Component
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public Mono<ProviderResult> charge(ProviderCharge charge) {
        boolean fail = charge.simulateFailure()
                || (charge.vpa() != null && charge.vpa().toLowerCase().contains("failure"));

        // Simulate provider latency reactively (no blocking).
        return Mono.delay(Duration.ofMillis(150))
                .map(t -> {
                    if (fail) {
                        log.info("Mock PSP declined charge for {}", charge.orderRef());
                        return ProviderResult.failed("Payment declined by issuer (mock)");
                    }
                    String ref = "mockpsp_" + CryptoService.randomToken(14);
                    log.info("Mock PSP captured {} {} for {} -> {}",
                            charge.amount(), charge.currency(), charge.orderRef(), ref);
                    return ProviderResult.ok(ref);
                });
    }
}
