package com.docsynth.domain.drift;

import com.docsynth.domain.tenant.TenantId;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * DriftItem — one row per detected change within a DriftReport.
 * Per data-model.md §DriftItem.
 */
@Entity
@Table(name = "drift_items")
public class DriftItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "drift_report_id", nullable = false)
    private UUID driftReportId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "change_kind", nullable = false)
    private String changeKind; // added|removed|changed

    @Column(name = "compatibility", nullable = false)
    private String compatibility; // breaking|non_breaking|informational

    @Column(name = "target_kind", nullable = false)
    private String targetKind; // endpoint|schema|parameter|response|security

    @Column(name = "target_path", nullable = false)
    private String targetPath;

    @Column(name = "detail", columnDefinition = "jsonb", nullable = false)
    private String detail;

    @Column(name = "message", nullable = false)
    private String message;

    protected DriftItem() {}

    public DriftItem(DriftReportId reportId, TenantId tenantId, DriftItemRecord rec) {
        this.driftReportId = reportId.value();
        this.tenantId = tenantId.value();
        this.changeKind = rec.changeKind();
        this.compatibility = rec.compatibility();
        this.targetKind = rec.targetKind();
        this.targetPath = rec.targetPath();
        this.message = rec.message();
        this.detail = "{}";
    }

    public UUID getId() { return id; }
    public UUID getDriftReportId() { return driftReportId; }
    public String getChangeKind() { return changeKind; }
    public String getCompatibility() { return compatibility; }
    public String getTargetKind() { return targetKind; }
    public String getTargetPath() { return targetPath; }
    public String getMessage() { return message; }
}
