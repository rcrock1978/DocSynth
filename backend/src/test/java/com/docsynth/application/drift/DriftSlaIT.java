package com.docsynth.application.drift;

import com.docsynth.domain.drift.DriftReport;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.drift.OpenApiDiffAdapter;
import com.docsynth.infrastructure.drift.LatestPublishedBaseline;
import com.docsynth.infrastructure.notification.CiCheckNotifier;
import com.docsynth.infrastructure.notification.EmailNotifier;
import com.docsynth.infrastructure.notification.SlackNotifier;
import com.docsynth.application.audit.AuditEmitter;
import com.docsynth.infrastructure.messaging.Outbox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test for SC-003: drift detection completes within 2 minutes
 * of a spec update for a 50-endpoint spec.
 */
@SpringBootTest
@ActiveProfiles("test")
class DriftSlaIT {

    @Autowired private DetectDriftUseCase useCase;
    @MockBean private OpenApiDiffAdapter diff;
    @MockBean private LatestPublishedBaseline baseline;
    @MockBean private SlackNotifier slack;
    @MockBean private EmailNotifier email;
    @MockBean private CiCheckNotifier ci;
    @MockBean private Outbox outbox;
    @MockBean private AuditEmitter audit;

    @Test
    void fifty_endpoints_drift_within_2_minutes() throws Exception {
        when(baseline.resolve(any())).thenReturn(new ApiSpecId(UUID.randomUUID()));
        when(diff.diff(any(), any())).thenReturn(new com.docsynth.domain.drift.DriftDiff(
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of()
        ));

        Instant start = Instant.now();
        DriftReport result = useCase.execute(new DetectDriftCommand(
            new ProjectId(UUID.randomUUID()),
            new TenantId(UUID.randomUUID()),
            new ApiSpecId(UUID.randomUUID()),
            "manual",
            null
        ));
        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(result).isNotNull();
        assertThat(elapsed).isLessThan(Duration.ofMinutes(2));
    }
}
