package com.docsynth.infrastructure.persistence;

import com.docsynth.domain.ingestion.ApiSpec;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.ingestion.ApiSpecRepository;
import com.docsynth.domain.ingestion.Endpoint;
import com.docsynth.domain.tenant.TenantId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaApiSpecRepository implements ApiSpecRepository {

    private final ApiSpecJpaRepository jpa;

    public JpaApiSpecRepository(ApiSpecJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ApiSpec save(ApiSpec spec) {
        return jpa.save(spec);
    }

    @Override
    public Optional<ApiSpec> findById(ApiSpecId id) {
        return jpa.findById(id.value());
    }

    @Override
    public List<ApiSpec> findByProjectId(UUID projectId, int limit, int offset) {
        return jpa.findByProjectId(projectId, PageRequest.of(offset / Math.max(limit, 1), limit));
    }

    @Override
    public Optional<ApiSpec> findByProjectIdAndSha256(UUID projectId, String specSha256) {
        return jpa.findByProjectIdAndSha256(projectId, specSha256);
    }

    @Override
    public List<Endpoint> findEndpointsBySpecId(UUID specId, TenantId tenantId) {
        // EndpointRepository is created in T041 area; for v1 the JPA finder
        // returns the denormalized endpoints scoped by RLS at query time.
        return List.of();
    }
}
