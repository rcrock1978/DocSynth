package com.docsynth.infrastructure.persistence;

import com.docsynth.domain.drift.DriftItem;
import com.docsynth.domain.drift.DriftReport;
import com.docsynth.domain.drift.DriftReportId;
import com.docsynth.domain.drift.DriftReportRepository;
import com.docsynth.domain.tenant.TenantId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaDriftReportRepository implements DriftReportRepository {

    private final DriftReportJpaRepository reportJpa;
    private final DriftItemJpaRepository itemJpa;

    public JpaDriftReportRepository(DriftReportJpaRepository reportJpa, DriftItemJpaRepository itemJpa) {
        this.reportJpa = reportJpa;
        this.itemJpa = itemJpa;
    }

    @Override
    @Transactional
    public DriftReport save(DriftReport report, List<DriftItem> items) {
        DriftReport saved = reportJpa.save(report);
        for (DriftItem item : items) {
            // Reload items with the persisted report id; in-memory the item
            // carries the report UUID already.
            itemJpa.save(item);
        }
        return saved;
    }

    @Override
    public Optional<DriftReport> findById(DriftReportId id) {
        return reportJpa.findById(id.value());
    }

    @Override
    public List<DriftReport> findByProjectId(UUID projectId, int limit, int offset) {
        return reportJpa.findByProjectIdOrderByGeneratedAtDesc(projectId, PageRequest.of(0, limit));
    }

    @Override
    public List<DriftItem> findItemsByReportId(DriftReportId id, TenantId tenantId) {
        return itemJpa.findByDriftReportId(id.value());
    }

    @Override
    public Optional<DriftReport> findByLeftAndRightSpecId(UUID leftSpecId, UUID rightSpecId) {
        return reportJpa.findByLeftSpecIdAndRightSpecId(leftSpecId, rightSpecId);
    }
}
