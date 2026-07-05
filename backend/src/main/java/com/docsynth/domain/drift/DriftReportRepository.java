package com.docsynth.domain.drift;

import com.docsynth.domain.tenant.TenantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriftReportRepository {
    DriftReport save(DriftReport report, List<DriftItem> items);
    Optional<DriftReport> findById(DriftReportId id);
    List<DriftReport> findByProjectId(UUID projectId, int limit, int offset);
    List<DriftItem> findItemsByReportId(DriftReportId id, TenantId tenantId);
    Optional<DriftReport> findByLeftAndRightSpecId(UUID leftSpecId, UUID rightSpecId);
}
