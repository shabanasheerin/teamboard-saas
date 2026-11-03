package com.teamboard.service;

import com.teamboard.dto.Dtos.ChangePasswordRequest;
import com.teamboard.dto.Dtos.ResetPasswordRequest;
import com.teamboard.dto.Dtos.UserCreateRequest;
import com.teamboard.dto.Dtos.UserResponse;
import com.teamboard.entity.Role;
import com.teamboard.entity.User;
import com.teamboard.exception.ApiException;
import com.teamboard.repository.UserRepository;
import com.teamboard.security.AuthUser;
import com.teamboard.security.AuthUserService;
import com.teamboard.security.PasswordPolicy;
import com.teamboard.util.AfterCommit;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthUserService authUserService;
    private final RefreshTokenService refreshTokens;
    private final AuditService audit;

    public UserService(UserRepository users, PasswordEncoder encoder, AuthUserService authUserService,
                       RefreshTokenService refreshTokens, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.authUserService = authUserService;
        this.refreshTokens = refreshTokens;
        this.audit = audit;
    }

    /** Admin sees everyone in the organization. Others see active people only (for assignee lists). */
    @Transactional(readOnly = true)
    public List<UserResponse> list(AuthUser me) {
        List<User> list = me.admin()
                ? users.findByOrganizationIdOrderByNameAsc(me.orgId())
                : users.findByOrganizationIdAndActiveTrueOrderByNameAsc(me.orgId());
        return list.stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse me(AuthUser me) {
        return UserResponse.from(load(me, me.id()));
    }

    /** The admin adds a person with a temporary password. They must change it at first login. */
    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "#me.orgId()")
    public UserResponse create(AuthUser me, UserCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        PasswordPolicy.validate(request.password());

        User u = new User();
        u.setOrganization(load(me, me.id()).getOrganization());
        u.setName(request.name().trim());
        u.setEmail(email);
        u.setPassword(encoder.encode(request.password()));
        u.setRole(request.role());
        u.setMustChangePassword(true);
        u = users.save(u);

        audit.log(me, "USER", u.getId(), "USER_CREATED", u.getName() + " added as " + u.getRole());
        return UserResponse.from(u);
    }

    @Transactional
    public UserResponse changeRole(AuthUser me, Long id, Role role) {
        if (me.id().equals(id)) {
            throw ApiException.badRequest("You cannot change your own role");
        }
        User u = load(me, id);
        Role old = u.getRole();
        u.setRole(role);
        evictAfterCommit(u);
        audit.log(me, "USER", u.getId(), "ROLE_CHANGED", u.getName() + ": " + old + " to " + role);
        return UserResponse.from(u);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "#me.orgId()")
    public UserResponse toggleActive(AuthUser me, Long id) {
        if (me.id().equals(id)) {
            throw ApiException.badRequest("You cannot deactivate your own account");
        }
        User u = load(me, id);
        u.setActive(!u.isActive());
        if (!u.isActive()) {
            refreshTokens.revokeAll(u.getId());
        }
        evictAfterCommit(u);
        audit.log(me, "USER", u.getId(), u.isActive() ? "USER_ACTIVATED" : "USER_DEACTIVATED", u.getName());
        return UserResponse.from(u);
    }

    @Transactional
    public void changeOwnPassword(AuthUser me, ChangePasswordRequest request) {
        User u = load(me, me.id());
        if (!encoder.matches(request.currentPassword(), u.getPassword())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        PasswordPolicy.validate(request.newPassword());
        if (encoder.matches(request.newPassword(), u.getPassword())) {
            throw ApiException.badRequest("New password must be different from the current one");
        }
        u.setPassword(encoder.encode(request.newPassword()));
        u.setMustChangePassword(false);
        evictAfterCommit(u);
    }

    /** Admin sets a temporary password; the person must change it at next login. */
    @Transactional
    public void resetPassword(AuthUser me, Long id, ResetPasswordRequest request) {
        User u = load(me, id);
        PasswordPolicy.validate(request.newPassword());
        u.setPassword(encoder.encode(request.newPassword()));
        u.setMustChangePassword(true);
        refreshTokens.revokeAll(u.getId());
        evictAfterCommit(u);
        audit.log(me, "USER", u.getId(), "PASSWORD_RESET", "Temporary password set for " + u.getName());
    }

    /** Always searches inside the caller's organization, so other companies' users are "not found". */
    private User load(AuthUser me, Long id) {
        return users.findByIdAndOrganizationId(id, me.orgId()).orElseThrow(() -> ApiException.notFound("User"));
    }

    private void evictAfterCommit(User u) {
        String email = u.getEmail();
        AfterCommit.run(() -> authUserService.evict(email));
    }
}
