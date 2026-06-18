package com.microservices.payment.ratelimit;

import com.microservices.common.constant.Constants;
import com.microservices.payment.config.RateLimitProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis-backed fixed-window rate limiter applied to all payment endpoints.
 *
 * <p>Keyed by the authenticated user (via the {@code X-User-Id} header set by the
 * gateway) or the client IP as a fallback. Fails open: if Redis is unavailable the
 * request is allowed and a warning is logged, so the cache is never a hard dependency.
 */
@Slf4j
@Component
public class RateLimitFilter implements WebFilter, Ordered {

    private final ReactiveStringRedisTemplate redis;
    private final RateLimitProperties properties;

    public RateLimitFilter(ReactiveStringRedisTemplate redis, RateLimitProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.isEnabled() || !isRateLimited(exchange)) {
            return chain.filter(exchange);
        }

        long windowSeconds = properties.getWindow().toSeconds();
        long windowId = System.currentTimeMillis() / 1000 / windowSeconds;
        String key = "rl:" + resolveIdentifier(exchange.getRequest()) + ":" + windowId;

        return redis.opsForValue().increment(key)
                .flatMap(count -> {
                    Mono<Void> ttl = count == 1
                            ? redis.expire(key, Duration.ofSeconds(windowSeconds)).then()
                            : Mono.empty();
                    return ttl.then(Mono.defer(() -> {
                        if (count > properties.getLimit()) {
                            return tooManyRequests(exchange);
                        }
                        return chain.filter(exchange);
                    }));
                })
                .onErrorResume(ex -> {
                    log.warn("Rate limiter unavailable, allowing request: {}", ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    private boolean isRateLimited(ServerWebExchange exchange) {
        return exchange.getRequest().getPath().value().startsWith("/api/payments");
    }

    private String resolveIdentifier(ServerHttpRequest request) {
        String userId = request.getHeaders().getFirst(Constants.Headers.USER_ID);
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }
        return "ip:" + (request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown");
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
