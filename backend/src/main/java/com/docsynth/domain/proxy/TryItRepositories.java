package com.docsynth.domain.proxy;

import com.docsynth.domain.tenant.TenantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TryItAllowlistRepository {
    TryItAllowlistEntry save(TryItAllowlistEntry entry);
    List<TryItAllowlistEntry> findActiveByProjectId(UUID projectId);
    Optional<TryItAllowlistEntry> findById(UUID id);
    void revoke(UUID id);
}

interface TryItSecretRepository {
    TryItSecret save(TryItSecret secret);
    Optional<TryItSecret> findById(UUID id);
    List<TryItSecret> findByProjectId(UUID projectId, TenantId tenantId);
    void markRotated(UUID id);
}
