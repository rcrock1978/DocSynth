package com.docsynth.domain.drift;

import com.docsynth.domain.tenant.TenantId;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * DriftReport — comparison result between two ApiSpecs (or between a spec
 * and a published DocSet). Per data-model.md §DriftReport.
 */
@Entity
@Table(name = "drift_reports")
public class DriftReport {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "left_spec_id", nullable = false)
    private UUID leftSpecId;

    @Column(name = "right_spec_id", nullable = false)
    private UUID rightSpecId;

    @Column(name = "trigger", nullable = false)
    private String trigger; // scheduled|webhook|manual|publish

    @Column(name = "triggered_by_user_id")
    private UUID triggeredByUserId;

    @Column(name = "summary", columnDefinition = "jsonb", nullable = false)
    private String summary;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @Column(name = "report_uri")
    private String reportUri;

    @Column(name = "notification_status", nullable = false)
    private String notificationStatus = "pending";

    protected DriftReport() {}

    public DriftReport(
        ProjectId projectId, TenantId tenantId,
        ApiSpecIdRef leftSpec, ApiSpecIdRef rightSpec,
        String trigger, UUID triggeredByUserId,
        DriftSummary summary
    ) {
        this.projectId = projectId.value();
        this.tenantId = tenantId.value();
        this.leftSpecId = leftSpec.value();
        this.rightSpecId = rightSpec.value();
        this.trigger = trigger;
        this.triggeredByUserId = triggeredByUserId;
        this.summary = summaryJson(summary);
    }

    public static DriftReport reconstitute(
        DriftReportId id, ProjectId projectId, TenantId tenantId,
        ApiSpecIdRef leftSpec, ApiSpecIdRef rightSpec,
        String trigger, UUID triggeredByUserId,
        DriftSummary summary, Instant generatedAt,
        String reportUri, String notificationStatus
    ) {
        DriftReport r = new DriftReport(projectId, tenantId, leftSpec, rightSpec, trigger, triggeredByUserId, summary);
        r.id = id.value();
        r.generatedAt = generatedAt;
        r.reportUri = reportUri;
        r.notificationStatus = notificationStatus;
        return r;
    }

    public DriftReportId getId() { return new DriftReportId(id); }
    public ProjectId getProjectId() { return new ProjectId(projectId); }
    public TenantId getTenantId() { return new TenantId(tenantId); }
    public ApiSpecIdRef getLeftSpecId() { return new ApiSpecIdRef(leftSpecId); }
    public ApiSpecIdRef getRightSpecId() { return new ApiSpecIdRef(rightSpecId); }
    public String getTrigger() { return trigger; }
    public UUID getTriggeredByUserId() { return triggeredByUserId; }
    public DriftSummary summary() {
        return parseSummary(summary);
    }
    public String summaryJson() { return summary; }
    public Instant getGeneratedAt() { return generatedAt; }
    public String getReportUri() { return reportUri; }
    public String getNotificationStatus() { return notificationStatus; }

    public void markNotificationsSent() {
        this.notificationStatus = "sent";
    }
    public void markNotificationsFailed() {
        this.notificationStatus = "failed";
    }

    private static String summaryJson(DriftSummary s) {
        return "{\"added\":" + s.added()
            + ",\"removed\":" + s.removed()
            + ",\"changed\":" + s.changed()
            + ",\"breaking\":" + s.breaking() + "}";
    }
    private static DriftSummary parseSummary(String json) {
        // Minimal hand-rolled parse to avoid a hard Jackson dep in the domain layer.
        return new DriftSummary(
            extractInt(json, "added"),
            extractInt(json, "removed"),
            extractInt(json, "changed"),
            extractInt(json, "breaking")
        );
    }
    private static int extractInt(String json, String key) {
        int i = json.indexOf("\"" + key + "\":");
        if (i < 0) return 0;
        int start = i + key.length() + 3;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return Integer.parseInt(json.substring(start, end));
    }
}
