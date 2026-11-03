package com.teamboard.repository;

import com.teamboard.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    boolean existsBySlug(String slug);
}
