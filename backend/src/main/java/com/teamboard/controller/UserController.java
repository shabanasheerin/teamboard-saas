package com.teamboard.controller;

import com.teamboard.dto.Dtos.ChangePasswordRequest;
import com.teamboard.dto.Dtos.ResetPasswordRequest;
import com.teamboard.dto.Dtos.RoleRequest;
import com.teamboard.dto.Dtos.UserCreateRequest;
import com.teamboard.dto.Dtos.UserResponse;
import com.teamboard.security.AuthUser;
import com.teamboard.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list(@AuthenticationPrincipal AuthUser me) {
        return userService.list(me);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser me) {
        return userService.me(me);
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(me, request);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UserCreateRequest request) {
        return userService.create(me, request);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeRole(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                   @Valid @RequestBody RoleRequest request) {
        return userService.changeRole(me, id, request.role());
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse toggleActive(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return userService.toggleActive(me, id);
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                              @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(me, id, request);
    }
}
