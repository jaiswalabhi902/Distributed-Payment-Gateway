package com.microservices.gateway.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Gateway-side JWT validation settings, bound from {@code app.security.jwt}.
 */
@Data
@ConfigurationProperties(prefix = "app.security.jwt")
public class GatewayJwtProperties {

    /** Expected issuer claim; must match what auth-service signs. */
    private String issuer = "auth-service";

    /** Filesystem path to the RS256 public key (PEM) used to verify tokens. */
    private String publicKeyLocation = "./keys/public_key.pem";

    /** Ant-style path patterns that bypass authentication. */
    private List<String> publicPaths = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/health",
            "/actuator/**"
    );
}
