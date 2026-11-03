package com.teamboard.repository;

import com.teamboard.entity.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOrganizationIdOrderByNameAsc(Long organizationId);

    Optional<Project> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByOrganizationIdAndProjectKey(Long organizationId, String projectKey);

    long countByOrganizationIdAndArchivedFalse(Long organizationId);

    /** Locks the row so two people creating tasks at the same time get different task numbers. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Project p where p.id = :id and p.organization.id = :orgId")
    Optional<Project> findForUpdate(@Param("id") Long id, @Param("orgId") Long orgId);
}
