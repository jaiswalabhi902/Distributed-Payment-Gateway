package com.microservices.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Verifies RS256 access tokens at the edge using the auth-service public key.
 * The key is loaded once at startup from the configured PEM location (shared
 * volume in deployment).
 */
@Slf4j
@Component
public class JwtValidator {

    private final GatewayJwtProperties properties;
    private final RSAPublicKey publicKey;

    public JwtValidator(GatewayJwtProperties properties) throws Exception {
        this.properties = properties;
        this.publicKey = loadPublicKey(properties.getPublicKeyLocation());
        log.info("Loaded JWT public key from {}", properties.getPublicKeyLocation());
    }

    public Claims validate(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private RSAPublicKey loadPublicKey(String location) throws Exception {
        String pem = Files.readString(Path.of(location));
        String base64 = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(der));
    }
}
