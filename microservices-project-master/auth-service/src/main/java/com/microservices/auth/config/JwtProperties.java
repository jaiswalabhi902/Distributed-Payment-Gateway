package com.microservices.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT and RSA key settings, bound from the {@code app.security.jwt} config prefix.
 */
@Data
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    /** Token issuer claim. */
    private String issuer = "auth-service";

    /** Access token lifetime. */
    private Duration accessTokenExpiration = Duration.ofMinutes(15);

    /** Refresh token lifetime. */
    private Duration refreshTokenExpiration = Duration.ofDays(7);

    /**
     * Directory where the RSA key pair is read from / persisted to.
     * Mount real keys here in production; a pair is generated on first run otherwise.
     */
    private String keyDirectory = "./keys";
}
