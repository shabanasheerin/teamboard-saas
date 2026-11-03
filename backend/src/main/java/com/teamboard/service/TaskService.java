package com.teamboard.service;

import com.teamboard.dto.Dtos.TaskRequest;
import com.teamboard.dto.Dtos.TaskResponse;
import com.teamboard.entity.Project;
import com.teamboard.entity.Task;
import com.teamboard.entity.TaskStatus;
import com.teamboard.entity.User;
import com.teamboard.exception.ApiException;
import com.teamboard.realtime.RealtimePublisher;
import com.teamboard.repository.ProjectRepository;
import com.teamboard.repository.TaskRepository;
import com.teamboard.repository.UserRepository;
import com.teamboard.security.AuthUser;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Tenant safety: every lookup uses the caller's organization id (me.orgId()).
 * A task or project that belongs to another company is reported as "not found", never as "forbidden".
 *
 * Who can do what:
 *  - Admin and Manager: create, edit, reassign, move and delete any task
 *  - Member: create tasks, edit tasks they reported, move tasks assigned to or reported by them
 */
@Service
public class TaskService {

    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final AuditService audit;
    private final NotificationService notifications;
    private final RealtimePublisher realtime;

    public TaskService(TaskRepository tasks, ProjectRepository projects, UserRepository users,
                       AuditService audit, NotificationService notifications, RealtimePublisher realtime) {
        this.tasks = tasks;
        this.projects = projects;
        this.users = users;
        this.audit = audit;
        this.notifications = notifications;
        this.realtime = realtime;
    }

    // ---------- queries ----------

    @Transactional(readOnly = true)
    public List<TaskResponse> listByProject(AuthUser me, Long projectId) {
        projects.findByIdAndOrganizationId(projectId, me.orgId()).orElseThrow(() -> ApiException.notFound("Project"));
        return tasks.findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(projectId, me.orgId()).stream()
                .map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> mine(AuthUser me) {
        return tasks.findByOrganizationIdAndAssigneeIdAndStatusNotOrderByDueDateAsc(
                me.orgId(), me.id(), TaskStatus.DONE).stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(AuthUser me, Long id) {
        return TaskResponse.from(find(me, id));
    }

    // ---------- commands ----------

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "#me.orgId()")
    public TaskResponse create(AuthUser me, Long projectId, TaskRequest request) {
        Project project = projects.findForUpdate(projectId, me.orgId())
                .orElseThrow(() -> ApiException.notFound("Project"));
        if (project.isArchived()) {
            throw ApiException.badRequest("This project is archived. Restore it to add tasks.");
        }
        User assignee = resolveAssignee(me, request.assigneeId());
        User reporter = users.findById(me.id()).orElseThrow(() -> ApiException.notFound("User"));

        project.setTaskCounter(project.getTaskCounter() + 1);

        Task task = new Task();
        task.setOrganization(project.getOrganization());
        task.setProject(project);
        task.setTaskNumber(project.getTaskCounter());
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setAssignee(assignee);
        task.setReporter(reporter);
        task = tasks.save(task);

        audit.log(me, "TASK", task.getId(), "TASK_CREATED", task.displayKey() + " " + task.getTitle());
        if (assignee != null && !assignee.getId().equals(me.id())) {
            notifications.notify(assignee, me.name() + " assigned you " + task.displayKey() + ": " + task.getTitle(),
                    task.getId(), projectId);
        }
        realtime.taskChanged(me.orgId(), "CREATED", task.getId(), projectId);
        return TaskResponse.from(task);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "#me.orgId()")
    public TaskResponse update(AuthUser me, Long id, TaskRequest request) {
        Task task = find(me, id);
        if (!me.canManage() && !task.getReporter().getId().equals(me.id())) {
            throw ApiException.forbidden("You can only edit tasks you created");
        }
        User oldAssignee = task.getAssignee();
        User newAssignee = resolveAssignee(me, request.assigneeId());

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setAssignee(newAssignee);

        audit.log(me, "TASK", task.getId(), "TASK_UPDATED", task.displayKey() + " edited");
        Long oldId = oldAssignee == null ? null : oldAssignee.getId();
        Long newId = newAssignee == null ? null : newAssignee.getId();
        if (!Objects.equals(oldId, newId)) {
            audit.log(me, "TASK", task.getId(), "TASK_REASSIGNED",
                    task.displayKey() + ": " + (oldAssignee == null ? "nobody" : oldAssignee.getName())
                            + " to " + (newAssignee == null ? "nobody" : newAssignee.getName()));
            if (newAssignee != null && !newAssignee.getId().equals(me.id())) {
                notifications.notify(newAssignee,
                        me.name() + " assigned you " + task.displayKey() + ": " + task.getTitle(),
                        task.getId(), task.getProject().getId());
            }
        }
        realtime.taskChanged(me.orgId(), "UPDATED", task.getId(), task.getProject().getId());
        return TaskResponse.from(task);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "#me.orgId()")
    public TaskResponse updateStatus(AuthUser me, Long id, TaskStatus newStatus) {
        Task task = find(me, id);
        boolean isAssignee = task.getAssignee() != null && task.getAssignee().getId().equals(me.id());
        boolean isReporter = task.getReporter().getId().equals(me.id());
        if (!me.canManage() && !isAssignee && !isReporter) {
            throw ApiException.forbidden("You can only move tasks assigned to you or created by you");
        }
        TaskStatus old = task.getStatus();
        if (old == newStatus) {
            return TaskResponse.from(task);
        }
        task.setStatus(newStatus);

        audit.log(me, "TASK", task.getId(), "STATUS_CHANGED", task.displayKey() + ": " + old + " to " + newStatus);
        notifications.notifyParticipants(me, task,
                me.name() + " moved " + task.displayKey() + " to " + newStatus);
        realtime.taskChanged(me.orgId(), "STATUS", task.getId(), task.getProject().getId());
        return TaskResponse.from(task);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "#me.orgId()")
    public void delete(AuthUser me, Long id) {
        if (!me.canManage()) {
            throw ApiException.forbidden("Only managers and admins can delete tasks");
        }
        Task task = find(me, id);
        Long projectId = task.getProject().getId();
        audit.log(me, "TASK", task.getId(), "TASK_DELETED", task.displayKey() + " " + task.getTitle());
        tasks.delete(task);
        realtime.taskChanged(me.orgId(), "DELETED", id, projectId);
    }

    // ---------- helpers ----------

    private Task find(AuthUser me, Long id) {
        return tasks.findByIdAndOrganizationId(id, me.orgId()).orElseThrow(() -> ApiException.notFound("Task"));
    }

    /** The assignee must be an active person in the same organization (or nobody). */
    private User resolveAssignee(AuthUser me, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return users.findByIdAndOrganizationId(assigneeId, me.orgId())
                .filter(User::isActive)
                .orElseThrow(() -> ApiException.badRequest("The assignee must be an active person in your organization"));
    }
}
