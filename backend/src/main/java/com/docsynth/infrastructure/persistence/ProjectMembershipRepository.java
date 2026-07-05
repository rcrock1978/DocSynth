package com.docsynth.infrastructure.persistence;

import com.docsynth.domain.project.ProjectMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, UUID> {

    @Query("SELECT pm FROM ProjectMembership pm WHERE pm.projectId = :projectId AND pm.userId = :userId")
    Optional<ProjectMembership> findByProjectAndUser(UUID projectId, UUID userId);
}
