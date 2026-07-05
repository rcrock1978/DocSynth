package com.docsynth.application.drift;

import com.docsynth.domain.drift.ApiSpecIdRef;
import com.docsynth.domain.drift.DriftDiff;
import com.docsynth.domain.drift.DriftItem;
import com.docsynth.domain.drift.DriftItemRecord;
import com.docsynth.domain.drift.DriftReport;
import com.docsynth.domain.drift.DriftReportId;
import com.docsynth.domain.drift.DriftReportRepository;
import com.docsynth.domain.drift.DriftSummary;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.notification.Notifier;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.drift.LatestPublishedBaseline;
import com.docsynth.infrastructure.drift.OpenApiDiffAdapter;
import com.docsynth.application.audit.AuditEventEnvelope;
import com.docsynth.application.audit.AuditEmitter;
import com.docsynth.infrastructure.messaging.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DetectDriftUseCase — orchestrates drift detection (FR-004, SC-003).
 *
 * Steps:
 *  1. Resolve baseline spec (latest published DocSet's source ApiSpec).
 *  2. Diff left (newest) vs right (baseline) via OpenApiDiffAdapter.
 *  3. Persist DriftReport + DriftItems.
 *  4. Fan out to configured Notifier channels (Slack/Email/CI).
 *  5. Outbox + audit.
 */
@Service
public class DetectDriftUseCase {

    private static final Logger log = LoggerFactory.getLogger(DetectDriftUseCase.class);

    private final OpenApiDiffAdapter diff;
    private final LatestPublishedBaseline baseline;
    private final List<Notifier> notifiers;
    private final DriftReportRepository repository;
    private final Outbox outbox;
    private final AuditEmitter audit;

    public DetectDriftUseCase(
        OpenApiDiffAdapter diff,
        LatestPublishedBaseline baseline,
        List<Notifier> notifiers,
        DriftReportRepository repository,
        Outbox outbox,
        AuditEmitter audit
    ) {
        this.diff = diff;
        this.baseline = baseline;
        this.notifiers = notifiers;
        this.repository = repository;
        this.outbox = outbox;
        this.audit = audit;
    }

    @Transactional
    public DriftReport execute(DetectDriftCommand cmd) {
        ApiSpecId baselineSpec = baseline.resolve(cmd.projectId());
        if (baselineSpec == null) {
            log.info("no baseline yet for project {}; skipping drift", cmd.projectId().value());
            return null;
        }

        DriftDiff drift = diff.diff(new ApiSpecIdRef(cmd.leftSpecId().value()),
                                     new ApiSpecIdRef(baselineSpec.value()));

        // Dedup: if a report already exists for the same (left, right) pair, return it.
        var existing = repository.findByLeftAndRightSpecId(
            cmd.leftSpecId().value(), baselineSpec.value());
        if (existing.isPresent()) {
            log.info("drift report already exists for ({}, {})", cmd.leftSpecId(), baselineSpec);
            return existing.get();
        }

        DriftSummary summary = new DriftSummary(
            drift.added().size(),
            drift.removed().size(),
            drift.changed().size(),
            drift.breakingCount()
        );

        DriftReport report = new DriftReport(
            cmd.projectId(), cmd.tenantId(),
            new ApiSpecIdRef(cmd.leftSpecId().value()),
            new ApiSpecIdRef(baselineSpec.value()),
            cmd.trigger(), cmd.actorUserId(),
            summary
        );

        List<DriftItem> items = drift.all().stream()
            .map(rec -> new DriftItem(report.getId(), cmd.tenantId(), rec))
            .toList();
        DriftReport saved = repository.save(report, items);

        // Fan out notifications.
        boolean anySent = false;
        for (Notifier notifier : notifiers) {
            try {
                notifier.send(saved, Map.of("kind", notifier.kind()));
                anySent = true;
            } catch (Exception e) {
                log.warn("notifier {} failed: {}", notifier.kind(), e.getMessage());
            }
        }
        if (anySent) {
            saved.markNotificationsSent();
        } else if (drift.totalCount() > 0) {
            saved.markNotificationsFailed();
        }

        outbox.append("drift.detect", "drift_report", saved.getId().value(), Map.of(
            "reportId", saved.getId().value().toString(),
            "projectId", saved.getProjectId().value().toString(),
            "tenantId", saved.getTenantId().value().toString(),
            "summary", summary
        ));

        audit.emit(new AuditEventEnvelope(
            cmd.tenantId(),
            cmd.actorUserId() == null ? null : new com.docsynth.domain.user.UserId(cmd.actorUserId()),
            "detect_drift",
            "drift_report",
            saved.getId().value(),
            saved.getProjectId().value(),
            anySent ? AuditEventEnvelope.Outcome.SUCCESS
                    : (drift.totalCount() == 0 ? AuditEventEnvelope.Outcome.SUCCESS
                                               : AuditEventEnvelope.Outcome.FAILURE),
            Map.of(
                "trigger", cmd.trigger(),
                "added", summary.added(),
                "removed", summary.removed(),
                "changed", summary.changed(),
                "breaking", summary.breaking()
            )
        ));

        return saved;
    }
}
