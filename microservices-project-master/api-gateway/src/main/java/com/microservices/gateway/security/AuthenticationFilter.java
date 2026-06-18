package com.microservices.gateway.security;

import com.microservices.common.constant.Constants;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates the JWT for protected routes and relays the verified identity to
 * downstream services via {@code X-User-*} headers. Client-supplied identity
 * headers are always stripped first to prevent spoofing.
 */
@Slf4j
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtValidator jwtValidator;
    private final GatewayJwtProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthenticationFilter(JwtValidator jwtValidator, GatewayJwtProperties properties) {
        this.jwtValidator = jwtValidator;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Always remove any inbound identity headers so they can't be forged.
        ServerHttpRequest sanitized = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(Constants.Headers.USER_ID);
                    h.remove(Constants.Headers.USERNAME);
                    h.remove(Constants.Headers.ROLES);
                })
                .build();
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitized).build();

        String path = sanitized.getPath().value();
        if (isPublic(path)) {
            return chain.filter(sanitizedExchange);
        }

        String token = resolveToken(sanitized);
        if (token == null) {
            return unauthorized(sanitizedExchange, "Missing or malformed Authorization header");
        }

        try {
            Claims claims = jwtValidator.validate(token);
            ServerHttpRequest authed = sanitized.mutate()
                    .header(Constants.Headers.USER_ID, String.valueOf(claims.get("userId")))
                    .header(Constants.Headers.USERNAME, claims.getSubject())
                    .header(Constants.Headers.ROLES, extractRoles(claims))
                    .build();
            return chain.filter(sanitizedExchange.mutate().request(authed).build());
        } catch (Exception e) {
            log.debug("JWT validation failed for {}: {}", path, e.getMessage());
            return unauthorized(sanitizedExchange, "Invalid or expired token");
        }
    }

    private boolean isPublic(String path) {
        return properties.getPublicPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String resolveToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private String extractRoles(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof Collection<?> collection) {
            return collection.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        return "";
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"success\":false,\"message\":\"" + message + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
