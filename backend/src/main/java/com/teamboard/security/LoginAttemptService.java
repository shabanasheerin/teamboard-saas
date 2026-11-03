package com.teamboard.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Blocks an email for 15 minutes after 5 wrong passwords. In memory: fine for one server instance. */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private record Attempt(int count, Instant first) {
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Attempt a = attempts.get(key);
        if (a == null) {
            return false;
        }
        if (a.first().plus(WINDOW).isBefore(Instant.now())) {
            attempts.remove(key);
            return false;
        }
        return a.count() >= MAX_FAILURES;
    }

    public void recordFailure(String key) {
        attempts.merge(key, new Attempt(1, Instant.now()), (old, fresh) ->
                old.first().plus(WINDOW).isBefore(Instant.now())
                        ? fresh
                        : new Attempt(old.count() + 1, old.first()));
    }

    public void reset(String key) {
        attempts.remove(key);
    }
}
