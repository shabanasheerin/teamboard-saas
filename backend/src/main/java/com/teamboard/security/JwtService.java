package com.teamboard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Creates and checks the short-lived access token (15 minutes). Refresh tokens are separate. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-expiration-ms}") long accessExpirationMs) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
    }

    public String generateAccessToken(AuthUser user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.email())
                .claim("role", user.role().name())
                .claim("org", String.valueOf(user.orgId()))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpirationMs))
                .signWith(key)
                .compact();
    }

    public long accessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }

    /** Throws JwtException if the token is invalid or expired. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
