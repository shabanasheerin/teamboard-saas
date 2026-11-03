package com.teamboard.service;

import com.teamboard.dto.Dtos.ProjectRequest;
import com.teamboard.dto.Dtos.ProjectResponse;
import com.teamboard.entity.Project;
import com.teamboard.entity.User;
import com.teamboard.exception.ApiException;
import com.teamboard.repository.ProjectRepository;
import com.teamboard.repository.TaskRepository;
import com.teamboard.repository.UserRepository;
import com.teamboard.security.AuthUser;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final UserRepository users;
    private final AuditService audit;

    public ProjectService(ProjectRepository projects, TaskRepository tasks, UserRepository users,
                          AuditService audit) {
        this.projects = projects;
        this.tasks = tasks;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(AuthUser me) {
        return projects.findByOrganizationIdOrderByNameAsc(me.orgId()).stream()
                .map(p -> ProjectResponse.from(p, tasks.countByProjectId(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(AuthUser me, Long id) {
        Project p = load(me, id);
        return ProjectResponse.from(p, tasks.countByProjectId(p.getId()));
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "#me.orgId()")
    public ProjectResponse create(AuthUser me, ProjectRequest request) {
        requireManager(me);
        if (request.key() == null || request.key().isBlank()) {
            throw ApiException.badRequest("A short project key such as WEB is required");
        }
        String key = request.key().trim().toUpperCase();
        if (projects.existsByOrganizationIdAndProjectKey(me.orgId(), key)) {
            throw new ApiException(HttpStatus.CONFLICT, "This organization already has a project with key " + key);
        }
        User creator = users.findById(me.id()).orElseThrow(() -> ApiException.notFound("User"));

        Project p = new Project();
        p.setOrganization(creator.getOrganization());
        p.setName(request.name().trim());
        p.setProjectKey(key);
        p.setDescription(request.description());
        p.setCreatedBy(creator);
        p = projects.save(p);

        audit.log(me, "PROJECT", p.getId(), "PROJECT_CREATED", p.getName() + " (" + key + ")");
        return ProjectResponse.from(p, 0);
    }

    @Transactional
    public ProjectResponse update(AuthUser me, Long id, ProjectRequest request) {
        requireManager(me);
        Project p = load(me, id);
        p.setName(request.name().trim());
        p.setDescription(request.description());
        audit.log(me, "PROJECT", p.getId(), "PROJECT_UPDATED", p.getName());
        return ProjectResponse.from(p, tasks.countByProjectId(p.getId()));
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "#me.orgId()")
    public ProjectResponse toggleArchive(AuthUser me, Long id) {
        requireManager(me);
        Project p = load(me, id);
        p.setArchived(!p.isArchived());
        audit.log(me, "PROJECT", p.getId(), p.isArchived() ? "PROJECT_ARCHIVED" : "PROJECT_RESTORED", p.getName());
        return ProjectResponse.from(p, tasks.countByProjectId(p.getId()));
    }

    private Project load(AuthUser me, Long id) {
        return projects.findByIdAndOrganizationId(id, me.orgId()).orElseThrow(() -> ApiException.notFound("Project"));
    }

    private void requireManager(AuthUser me) {
        if (!me.canManage()) {
            throw ApiException.forbidden("Only managers and admins can do this");
        }
    }
}
