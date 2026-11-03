package com.teamboard.controller;

import com.teamboard.dto.Dtos.ProjectRequest;
import com.teamboard.dto.Dtos.ProjectResponse;
import com.teamboard.dto.Dtos.TaskRequest;
import com.teamboard.dto.Dtos.TaskResponse;
import com.teamboard.security.AuthUser;
import com.teamboard.service.ProjectService;
import com.teamboard.service.TaskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;

    public ProjectController(ProjectService projectService, TaskService taskService) {
        this.projectService = projectService;
        this.taskService = taskService;
    }

    @GetMapping
    public List<ProjectResponse> list(@AuthenticationPrincipal AuthUser me) {
        return projectService.list(me);
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return projectService.get(me, id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ProjectRequest request) {
        return projectService.create(me, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProjectResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                  @Valid @RequestBody ProjectRequest request) {
        return projectService.update(me, id, request);
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProjectResponse toggleArchive(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return projectService.toggleArchive(me, id);
    }

    @GetMapping("/{id}/tasks")
    public List<TaskResponse> tasks(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return taskService.listByProject(me, id);
    }

    @PostMapping("/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                   @Valid @RequestBody TaskRequest request) {
        return taskService.create(me, id, request);
    }
}
