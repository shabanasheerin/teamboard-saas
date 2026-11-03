package com.teamboard.dto;

import com.teamboard.entity.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Every request and response object, so entities never leave the service layer. */
public final class Dtos {

    private Dtos() {
    }

    // ---------- auth ----------
    public record RegisterRequest(
            @NotBlank @Size(max = 100) String organizationName,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 60) String password) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(String accessToken, String refreshToken, long expiresInSeconds, UserResponse user) {
    }

    // ---------- users ----------
    public record UserResponse(Long id, String name, String email, Role role, boolean active,
                               boolean mustChangePassword, Long organizationId, String organizationName) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isActive(),
                    u.isMustChangePassword(), u.getOrganization().getId(), u.getOrganization().getName());
        }
    }

    public record UserCreateRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 60) String password,
            @NotNull Role role) {
    }

    public record RoleRequest(@NotNull Role role) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8, max = 60) String newPassword) {
    }

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 60) String newPassword) {
    }

    // ---------- projects ----------
    public record ProjectRequest(
            @NotBlank @Size(max = 120) String name,
            @Pattern(regexp = "[A-Za-z][A-Za-z0-9]{1,9}", message = "must be 2 to 10 letters or digits, starting with a letter")
            String key,
            @Size(max = 1000) String description) {
    }

    public record ProjectResponse(Long id, String name, String key, String description, boolean archived,
                                  long taskCount, String createdByName, LocalDateTime createdAt) {
        public static ProjectResponse from(Project p, long taskCount) {
            return new ProjectResponse(p.getId(), p.getName(), p.getProjectKey(), p.getDescription(),
                    p.isArchived(), taskCount, p.getCreatedBy().getName(), p.getCreatedAt());
        }
    }

    // ---------- tasks ----------
    public record TaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @NotNull Priority priority,
            LocalDate dueDate,
            Long assigneeId) {
    }

    public record StatusRequest(@NotNull TaskStatus status) {
    }

    public record TaskResponse(Long id, String key, Long projectId, String projectName, String title,
                               String description, TaskStatus status, Priority priority, LocalDate dueDate,
                               boolean overdue, Long assigneeId, String assigneeName,
                               Long reporterId, String reporterName,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static TaskResponse from(Task t) {
            boolean overdue = t.getDueDate() != null
                    && t.getDueDate().isBefore(LocalDate.now())
                    && t.getStatus() != TaskStatus.DONE;
            User a = t.getAssignee();
            return new TaskResponse(t.getId(), t.displayKey(), t.getProject().getId(), t.getProject().getName(),
                    t.getTitle(), t.getDescription(), t.getStatus(), t.getPriority(), t.getDueDate(), overdue,
                    a == null ? null : a.getId(), a == null ? null : a.getName(),
                    t.getReporter().getId(), t.getReporter().getName(),
                    t.getCreatedAt(), t.getUpdatedAt());
        }
    }

    // ---------- comments ----------
    public record CommentRequest(@NotBlank @Size(max = 1000) String text) {
    }

    public record CommentResponse(Long id, Long authorId, String authorName, String text, LocalDateTime createdAt) {
        public static CommentResponse from(Comment c) {
            return new CommentResponse(c.getId(), c.getAuthor().getId(), c.getAuthor().getName(),
                    c.getText(), c.getCreatedAt());
        }
    }

    // ---------- notifications / audit / dashboard ----------
    public record NotificationResponse(Long id, String message, boolean seen, Long taskId, Long projectId,
                                       LocalDateTime createdAt) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getMessage(), n.isSeen(), n.getTaskId(),
                    n.getProjectId(), n.getCreatedAt());
        }
    }

    public record AuditResponse(Long id, String entityType, Long entityId, String action, String details,
                                String actorName, LocalDateTime createdAt) {
        public static AuditResponse from(AuditLog a) {
            return new AuditResponse(a.getId(), a.getEntityType(), a.getEntityId(), a.getAction(),
                    a.getDetails(), a.getActorName(), a.getCreatedAt());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int totalPages, long totalElements) {
    }

    /** Serializable because it is stored in the (Redis) cache. */
    public record DashboardResponse(long total, long todo, long inProgress, long inReview, long done,
                                    long overdue, long projects, long members,
                                    Map<String, Long> openByPriority) implements Serializable {
    }

    // ---------- live (WebSocket) payloads: plain strings, no date types ----------
    public record TaskEvent(String type, Long taskId, Long projectId) {
    }

    public record NotificationPush(Long id, String message, Long taskId, Long projectId, String createdAt) {
    }
}
