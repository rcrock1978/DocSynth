package com.docsynth.infrastructure.persistence;

import com.docsynth.domain.proxy.TryItSecret;
import com.docsynth.domain.tenant.TenantId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TryItSecretJpaRepository extends JpaRepository<TryItSecret, UUID> {
    List<TryItSecret> findByProjectIdAndTenantId(UUID projectId, TenantId tenantId);
}
