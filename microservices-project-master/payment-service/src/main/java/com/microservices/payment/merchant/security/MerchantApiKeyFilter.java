package com.microservices.payment.merchant.security;

import com.microservices.payment.merchant.entity.Merchant;
import com.microservices.payment.merchant.service.MerchantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Authenticates merchant API calls under {@code /v1/**} using HTTP Basic auth where the
 * username is the {@code key_id} and the password is the {@code key_secret}. The
 * authenticated {@link Merchant} is placed on the exchange for controllers to read.
 */
@Slf4j
@Component
public class MerchantApiKeyFilter implements WebFilter, Ordered {

    public static final String MERCHANT_ATTR = "merchant";
    private static final String BASIC_PREFIX = "Basic ";

    private final MerchantService merchantService;

    public MerchantApiKeyFilter(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith("/v1/")) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BASIC_PREFIX)) {
            return unauthorized(exchange, "Missing API key (HTTP Basic key_id:key_secret)");
        }

        String[] creds = decode(header.substring(BASIC_PREFIX.length()));
        if (creds == null) {
            return unauthorized(exchange, "Malformed Authorization header");
        }

        return merchantService.authenticate(creds[0], creds[1])
                .flatMap(merchant -> {
                    exchange.getAttributes().put(MERCHANT_ATTR, merchant);
                    return chain.filter(exchange);
                })
                .switchIfEmpty(Mono.defer(() -> unauthorized(exchange, "Invalid API key")));
    }

    private String[] decode(String base64) {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            int sep = decoded.indexOf(':');
            if (sep < 0) return null;
            return new String[] {decoded.substring(0, sep), decoded.substring(sep + 1)};
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"success\":false,\"message\":\"" + message + "\"}";
        DataBuffer buf = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buf));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
