package com.docsynth.domain.ingestion;

import com.docsynth.domain.tenant.TenantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for ApiSpec aggregate. Implemented in infrastructure.
 */
public interface ApiSpecRepository {
    ApiSpec save(ApiSpec spec);
    Optional<ApiSpec> findById(ApiSpecId id);
    List<ApiSpec> findByProjectId(UUID projectId, int limit, int offset);
    Optional<ApiSpec> findByProjectIdAndSha256(UUID projectId, String specSha256);
    List<Endpoint> findEndpointsBySpecId(UUID specId, TenantId tenantId);
}
