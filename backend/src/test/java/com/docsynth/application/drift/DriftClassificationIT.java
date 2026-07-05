package com.docsynth.application.drift;

import com.docsynth.domain.drift.*;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.drift.LatestPublishedBaseline;
import com.docsynth.infrastructure.drift.OpenApiDiffAdapter;
import com.docsynth.infrastructure.notification.*;
import com.docsynth.application.audit.AuditEmitter;
import com.docsynth.infrastructure.messaging.Outbox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies the added/removed/changed classification and the breaking
 * compatibility dimension produced by the diff adapter.
 */
@SpringBootTest
@ActiveProfiles("test")
class DriftClassificationIT {

    @Autowired private DetectDriftUseCase useCase;
    @MockBean private OpenApiDiffAdapter diff;
    @MockBean private LatestPublishedBaseline baseline;
    @MockBean private SlackNotifier slack;
    @MockBean private EmailNotifier email;
    @MockBean private CiCheckNotifier ci;
    @MockBean private Outbox outbox;
    @MockBean private AuditEmitter audit;

    @Test
    void added_removed_changed_classified_correctly() {
        when(baseline.resolve(any())).thenReturn(new ApiSpecId(UUID.randomUUID()));
        when(diff.diff(any(), any())).thenReturn(new DriftDiff(
            List.of(new DriftItemRecord("endpoint", "GET /new", "added", "non_breaking", "New endpoint")),
            List.of(new DriftItemRecord("endpoint", "DELETE /old", "removed", "breaking", "Old removed")),
            List.of(new DriftItemRecord("parameter", "POST /x {body.email}", "changed", "breaking", "Email required"))
        ));

        DriftReport result = useCase.execute(new DetectDriftCommand(
            new ProjectId(UUID.randomUUID()),
            new TenantId(UUID.randomUUID()),
            new ApiSpecId(UUID.randomUUID()),
            "manual",
            null
        ));

        assertThat(result.summary().added()).isEqualTo(1);
        assertThat(result.summary().removed()).isEqualTo(1);
        assertThat(result.summary().changed()).isEqualTo(1);
        assertThat(result.summary().breaking()).isEqualTo(2);
    }
}
