package com.teamboard.service;

import com.teamboard.dto.Dtos.AuthResponse;
import com.teamboard.dto.Dtos.LoginRequest;
import com.teamboard.dto.Dtos.RegisterRequest;
import com.teamboard.dto.Dtos.UserResponse;
import com.teamboard.entity.Organization;
import com.teamboard.entity.Role;
import com.teamboard.entity.User;
import com.teamboard.exception.ApiException;
import com.teamboard.repository.OrganizationRepository;
import com.teamboard.repository.UserRepository;
import com.teamboard.security.AuthUser;
import com.teamboard.security.JwtService;
import com.teamboard.security.LoginAttemptService;
import com.teamboard.security.PasswordPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final LoginAttemptService attempts;
    private final AuditService audit;
    private final boolean registrationEnabled;

    public AuthService(UserRepository users, OrganizationRepository organizations, PasswordEncoder encoder,
                       JwtService jwtService, RefreshTokenService refreshTokens, LoginAttemptService attempts,
                       AuditService audit, @Value("${app.registration-enabled}") boolean registrationEnabled) {
        this.users = users;
        this.organizations = organizations;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
        this.attempts = attempts;
        this.audit = audit;
        this.registrationEnabled = registrationEnabled;
    }

    /** A new company signs up: creates its own workspace (tenant) and its first admin. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!registrationEnabled) {
            throw ApiException.forbidden("Sign-up is turned off on this server");
        }
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        PasswordPolicy.validate(request.password());

        Organization org = new Organization();
        org.setName(request.organizationName().trim());
        org.setSlug(uniqueSlug(org.getName()));
        org = organizations.save(org);

        User admin = new User();
        admin.setOrganization(org);
        admin.setName(request.name().trim());
        admin.setEmail(email);
        admin.setPassword(encoder.encode(request.password()));
        admin.setRole(Role.ADMIN);
        admin = users.save(admin);

        audit.log(AuthUser.from(admin), "ORGANIZATION", org.getId(), "ORG_CREATED",
                "Workspace '" + org.getName() + "' created");
        return issue(admin);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        if (attempts.isBlocked(email)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Please try again in 15 minutes.");
        }
        Optional<User> found = users.findByEmail(email)
                .filter(User::isActive)
                .filter(u -> encoder.matches(request.password(), u.getPassword()));
        if (found.isEmpty()) {
            attempts.recordFailure(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        attempts.reset(email);
        return issue(found.get());
    }

    /** Swaps a valid refresh token for a new access token and a new refresh token. */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(String refreshToken) {
        User user = refreshTokens.consume(refreshToken);
        return issue(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    private AuthResponse issue(User user) {
        String access = jwtService.generateAccessToken(AuthUser.from(user));
        String refresh = refreshTokens.issue(user);
        return new AuthResponse(access, refresh, jwtService.accessExpirationSeconds(), UserResponse.from(user));
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (base.isEmpty()) {
            base = "workspace";
        }
        if (base.length() > 60) {
            base = base.substring(0, 60);
        }
        String slug = base;
        int n = 2;
        while (organizations.existsBySlug(slug)) {
            slug = base + "-" + n++;
        }
        return slug;
    }
}
