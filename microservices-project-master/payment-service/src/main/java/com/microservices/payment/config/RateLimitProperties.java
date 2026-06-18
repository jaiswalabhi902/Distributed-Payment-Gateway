package com.microservices.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Fixed-window rate limit settings, bound from {@code app.rate-limit}.
 */
@Data
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /** Toggle rate limiting on/off. */
    private boolean enabled = true;

    /** Max requests allowed per window per identifier. */
    private int limit = 100;

    /** Length of the fixed window. */
    private Duration window = Duration.ofMinutes(1);
}
