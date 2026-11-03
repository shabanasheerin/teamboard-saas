package com.teamboard.repository;

import com.teamboard.entity.Priority;
import com.teamboard.entity.Task;
import com.teamboard.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // Tenant-scoped lookups: a task from another organization is simply "not found".
    Optional<Task> findByIdAndOrganizationId(Long id, Long organizationId);

    List<Task> findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(Long projectId, Long organizationId);

    List<Task> findByOrganizationIdAndAssigneeIdAndStatusNotOrderByDueDateAsc(
            Long organizationId, Long assigneeId, TaskStatus status);

    long countByProjectId(Long projectId);

    // dashboard counts
    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndStatus(Long organizationId, TaskStatus status);

    long countByOrganizationIdAndPriorityAndStatusNot(Long organizationId, Priority priority, TaskStatus status);

    long countByOrganizationIdAndDueDateBeforeAndStatusNot(Long organizationId, LocalDate date, TaskStatus status);

    // used by the daily reminder job (all organizations)
    List<Task> findByDueDateBeforeAndStatusNotAndAssigneeIsNotNull(LocalDate date, TaskStatus status);
}
