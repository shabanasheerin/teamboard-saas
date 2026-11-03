package com.teamboard.repository;

import com.teamboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Every query below is scoped to one organization (tenant).
    List<User> findByOrganizationIdOrderByNameAsc(Long organizationId);

    List<User> findByOrganizationIdAndActiveTrueOrderByNameAsc(Long organizationId);

    Optional<User> findByIdAndOrganizationId(Long id, Long organizationId);

    long countByOrganizationIdAndActiveTrue(Long organizationId);
}
