package com.docsynth.application.drift;

import com.docsynth.domain.drift.DriftReport;
import com.docsynth.domain.drift.DriftReportId;
import com.docsynth.domain.drift.DriftReportRepository;
import com.docsynth.domain.drift.DriftSummary;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test: Slack notification is dispatched on drift detection.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationIT {

    @Autowired private DetectDriftUseCase useCase;
    @MockBean private OpenApiDiffAdapter diff;
    @MockBean private LatestPublishedBaseline baseline;
    @MockBean private SlackNotifier slack;
    @MockBean private EmailNotifier email;
    @MockBean private CiCheckNotifier ci;
    @MockBean private Outbox outbox;
    @MockBean private AuditEmitter audit;

    @Test
    void slack_notifier_dispatched_on_drift() {
        when(baseline.resolve(any())).thenReturn(new ApiSpecId(UUID.randomUUID()));
        when(diff.diff(any(), any())).thenReturn(new com.docsynth.domain.drift.DriftDiff(
            List.of(), List.of(),
            List.of(new com.docsynth.domain.drift.DriftItemRecord(
                "endpoint", "GET /x", "changed", "non_breaking", "Cosmetic"))
        ));

        DriftReport result = useCase.execute(new DetectDriftCommand(
            new ProjectId(UUID.randomUUID()),
            new TenantId(UUID.randomUUID()),
            new ApiSpecId(UUID.randomUUID()),
            "manual",
            null
        ));

        verify(slack, times(1)).send(any(), any());
        verify(email, times(0)).send(any(), any());
        verify(ci, times(0)).send(any(), any());
        assertThat(result.notificationStatus()).isEqualTo("sent");
    }
}
