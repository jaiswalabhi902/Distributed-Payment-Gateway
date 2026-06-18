package com.microservices.auth.security;

import com.microservices.auth.config.JwtProperties;
import com.microservices.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Issues and validates RS256-signed access tokens.
 */
@Slf4j
@Service
public class JwtService {

    private final RsaKeyProvider keyProvider;
    private final JwtProperties properties;

    public JwtService(RsaKeyProvider keyProvider, JwtProperties properties) {
        this.keyProvider = keyProvider;
        this.properties = properties;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getAccessTokenExpiration());
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());

        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(keyProvider.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(keyProvider.getPublicKey())
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public long getAccessTokenExpirySeconds() {
        return properties.getAccessTokenExpiration().toSeconds();
    }
}
