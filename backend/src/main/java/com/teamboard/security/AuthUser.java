package com.teamboard.security;

import com.teamboard.entity.Role;
import com.teamboard.entity.User;

import java.io.Serializable;

/**
 * The logged-in person, as seen by controllers and services.
 * Small and serializable so it can live in the Redis cache.
 * orgId is the tenant: every query is scoped with it.
 */
public record AuthUser(Long id, Long orgId, String email, String name, Role role,
                       boolean active, boolean mustChangePassword) implements Serializable {

    public static AuthUser from(User u) {
        return new AuthUser(u.getId(), u.getOrganization().getId(), u.getEmail(), u.getName(),
                u.getRole(), u.isActive(), u.isMustChangePassword());
    }

    public boolean canManage() {
        return role == Role.ADMIN || role == Role.MANAGER;
    }

    public boolean admin() {
        return role == Role.ADMIN;
    }
}
