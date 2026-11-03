package com.teamboard.security;

import com.teamboard.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks the user up on every request (from the cache after the first time).
 * Whenever a user's role, status or password changes, call evict() so the change takes effect at once.
 */
@Service
public class AuthUserService {

    private final UserRepository users;

    public AuthUserService(UserRepository users) {
        this.users = users;
    }

    @Cacheable(cacheNames = "authUsers", key = "#email", unless = "#result == null")
    @Transactional(readOnly = true)
    public AuthUser findByEmail(String email) {
        return users.findByEmail(email).map(AuthUser::from).orElse(null);
    }

    @CacheEvict(cacheNames = "authUsers", key = "#email")
    public void evict(String email) {
        // nothing to do: the annotation removes the cached entry
    }
}
