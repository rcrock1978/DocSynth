package com.docsynth.application.ingestion;

import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.messaging.Outbox;
import com.docsynth.domain.ingestion.ApiSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes spec.parsed events from the outbox to trigger downstream work
 * (drift re-evaluation, doc-set generation hint, etc.).
 */
@Component
public class SpecParsedConsumer {

    private static final Logger log = LoggerFactory.getLogger(SpecParsedConsumer.class);

    private final Outbox outbox;
    // private final DriftReEvaluationTrigger driftTrigger; — wired in US3

    public SpecParsedConsumer(Outbox outbox) {
        this.outbox = outbox;
    }

    /**
     * Triggered when an ApiSpec is parsed. Schedules a drift re-evaluation
     * if a published DocSet exists for the project.
     */
    @EventListener
    public void onSpecParsed(SpecParsedEvent event) {
        log.info("spec.parsed received specId={} projectId={} endpointCount={}",
            event.specId(), event.projectId(), event.endpointCount());

        // Drift re-eval (US3 wiring).
        outbox.append("drift.detect", "api_spec", event.specId(), Map.of(
            "specId", event.specId().toString(),
            "projectId", event.projectId().toString(),
            "tenantId", event.tenantId().toString(),
            "reason", "spec_parsed"
        ));
    }

    public record SpecParsedEvent(
        ApiSpecId specId,
        ProjectId projectId,
        TenantId tenantId,
        int endpointCount,
        int schemaCount
    ) {}
}
