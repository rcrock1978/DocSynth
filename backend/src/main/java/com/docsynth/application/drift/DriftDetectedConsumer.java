package com.docsynth.application.drift;

import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.drift.LatestPublishedBaseline;
import com.docsynth.infrastructure.drift.OpenApiDiffAdapter;
import com.docsynth.infrastructure.notification.CiCheckNotifier;
import com.docsynth.infrastructure.notification.EmailNotifier;
import com.docsynth.infrastructure.notification.SlackNotifier;
import com.docsynth.application.audit.AuditEmitter;
import com.docsynth.infrastructure.messaging.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * DriftDetectedConsumer — consumes spec.parsed events and triggers
 * drift detection for the project. Idempotency key: (leftSpecId, baselineSpecId).
 */
@Component
public class DriftDetectedConsumer {

    private static final Logger log = LoggerFactory.getLogger(DriftDetectedConsumer.class);

    private final DetectDriftUseCase useCase;

    public DriftDetectedConsumer(DetectDriftUseCase useCase) {
        this.useCase = useCase;
    }

    @EventListener
    public void onSpecParsed(SpecParsedEventShim event) {
        try {
            useCase.execute(new DetectDriftCommand(
                event.projectId(),
                event.tenantId(),
                event.specId(),
                "webhook",
                null
            ));
        } catch (Exception e) {
            log.warn("drift detection failed for spec {}: {}", event.specId(), e.getMessage());
        }
    }

    /** Shim matching the SpecParsedConsumer.SpecParsedEvent shape. */
    public record SpecParsedEventShim(
        ApiSpecId specId,
        ProjectId projectId,
        TenantId tenantId,
        int endpointCount,
        int schemaCount
    ) {}
}
