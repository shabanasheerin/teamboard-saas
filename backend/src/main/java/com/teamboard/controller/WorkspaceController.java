package com.teamboard.controller;

import com.teamboard.dto.Dtos.AuditResponse;
import com.teamboard.dto.Dtos.DashboardResponse;
import com.teamboard.dto.Dtos.NotificationResponse;
import com.teamboard.dto.Dtos.PageResponse;
import com.teamboard.security.AuthUser;
import com.teamboard.service.AuditService;
import com.teamboard.service.DashboardService;
import com.teamboard.service.NotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Dashboard numbers, notifications and the audit log of the caller's organization. */
@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceController {

    private final DashboardService dashboardService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public WorkspaceController(DashboardService dashboardService, NotificationService notificationService,
                               AuditService auditService) {
        this.dashboardService = dashboardService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal AuthUser me) {
        return dashboardService.dashboard(me.orgId());
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> notifications(@AuthenticationPrincipal AuthUser me) {
        return notificationService.list(me);
    }

    @PatchMapping("/notifications/{id}/seen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markSeen(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        notificationService.markSeen(me, id);
    }

    @PatchMapping("/notifications/seen-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllSeen(@AuthenticationPrincipal AuthUser me) {
        notificationService.markAllSeen(me);
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PageResponse<AuditResponse> audit(@AuthenticationPrincipal AuthUser me,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "30") int size) {
        return auditService.list(me, page, size);
    }
}
