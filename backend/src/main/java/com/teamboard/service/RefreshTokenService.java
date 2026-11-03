package com.teamboard.service;

import com.teamboard.entity.RefreshToken;
import com.teamboard.entity.User;
import com.teamboard.exception.ApiException;
import com.teamboard.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Refresh tokens are random strings. Only their SHA-256 hash is stored, so a database leak
 * does not leak usable tokens. Each token is used once (rotation). Reusing an old one revokes everything.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final long validDays;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.jwt.refresh-expiration-days}") long validDays) {
        this.repository = repository;
        this.validDays = validDays;
    }

    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(validDays, ChronoUnit.DAYS));
        repository.save(token);
        return raw;
    }

    /** Checks the token, marks it used, and returns its owner. noRollbackFor keeps the "revoke all" on reuse. */
    @Transactional(noRollbackFor = ApiException.class)
    public User consume(String raw) {
        RefreshToken token = repository.findByTokenHash(hash(raw))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (token.isRevoked()) {
            // an old token was used again: assume it was stolen and sign the user out everywhere
            repository.revokeAllForUser(token.getUser().getId());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again.");
        }
        if (token.getExpiresAt().isBefore(Instant.now()) || !token.getUser().isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again.");
        }
        token.setRevoked(true);
        return token.getUser();
    }

    @Transactional
    public void revoke(String raw) {
        repository.findByTokenHash(hash(raw)).ifPresent(t -> t.setRevoked(true));
    }

    @Transactional
    public void revokeAll(Long userId) {
        repository.revokeAllForUser(userId);
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
