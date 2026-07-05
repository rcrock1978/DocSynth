package com.docsynth.infrastructure.persistence;

import com.docsynth.domain.ingestion.ApiSpec;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.tenant.TenantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiSpecJpaRepository extends JpaRepository<ApiSpec, UUID> {

    @Query("SELECT s FROM ApiSpec s WHERE s.projectId = :projectId ORDER BY s.parsedAt DESC")
    List<ApiSpec> findByProjectId(UUID projectId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT s FROM ApiSpec s WHERE s.projectId = :projectId AND s.specSha256 = :sha")
    Optional<ApiSpec> findByProjectIdAndSha256(UUID projectId, String sha);
}
