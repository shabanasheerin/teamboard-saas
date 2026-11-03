package com.teamboard.service;

import com.teamboard.dto.Dtos.DashboardResponse;
import com.teamboard.entity.Priority;
import com.teamboard.entity.TaskStatus;
import com.teamboard.repository.ProjectRepository;
import com.teamboard.repository.TaskRepository;
import com.teamboard.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final UserRepository users;

    public DashboardService(TaskRepository tasks, ProjectRepository projects, UserRepository users) {
        this.tasks = tasks;
        this.projects = projects;
        this.users = users;
    }

    /**
     * Cached per organization (in Redis when enabled). Task, project and user changes evict it,
     * and Redis also expires it after 5 minutes, so it can never stay wrong for long.
     */
    @Cacheable(cacheNames = "dashboard", key = "#orgId")
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(Long orgId) {
        Map<String, Long> openByPriority = new LinkedHashMap<>();
        for (Priority p : Priority.values()) {
            openByPriority.put(p.name(), tasks.countByOrganizationIdAndPriorityAndStatusNot(orgId, p, TaskStatus.DONE));
        }
        return new DashboardResponse(
                tasks.countByOrganizationId(orgId),
                tasks.countByOrganizationIdAndStatus(orgId, TaskStatus.TODO),
                tasks.countByOrganizationIdAndStatus(orgId, TaskStatus.IN_PROGRESS),
                tasks.countByOrganizationIdAndStatus(orgId, TaskStatus.IN_REVIEW),
                tasks.countByOrganizationIdAndStatus(orgId, TaskStatus.DONE),
                tasks.countByOrganizationIdAndDueDateBeforeAndStatusNot(orgId, LocalDate.now(), TaskStatus.DONE),
                projects.countByOrganizationIdAndArchivedFalse(orgId),
                users.countByOrganizationIdAndActiveTrue(orgId),
                openByPriority);
    }
}
