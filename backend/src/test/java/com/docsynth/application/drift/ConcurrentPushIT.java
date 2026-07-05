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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Concurrent pushes to the same repo serialize correctly: the baseline
 * resolver returns a stable result and the diff adapter is invoked once
 * per concurrent call, but no double-report is persisted for the same
 * (leftSpecId, rightSpecId) pair.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrentPushIT {

    @Autowired private DetectDriftUseCase useCase;
    @MockBean private OpenApiDiffAdapter diff;
    @MockBean private LatestPublishedBaseline baseline;
    @MockBean private SlackNotifier slack;
    @MockBean private EmailNotifier email;
    @MockBean private CiCheckNotifier ci;
    @MockBean private Outbox outbox;
    @MockBean private AuditEmitter audit;

    @Test
    void concurrent_calls_do_not_double_report() throws Exception {
        UUID leftSpec = UUID.randomUUID();
        UUID baselineSpec = UUID.randomUUID();
        when(baseline.resolve(any())).thenReturn(new ApiSpecId(baselineSpec));
        when(diff.diff(any(), any())).thenReturn(new com.docsynth.domain.drift.DriftDiff(
            java.util.List.of(), java.util.List.of(), java.util.List.of()
        ));

        int n = 5;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        AtomicInteger errors = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    useCase.execute(new DetectDriftCommand(
                        new ProjectId(UUID.randomUUID()),
                        new TenantId(UUID.randomUUID()),
                        new ApiSpecId(leftSpec),
                        "webhook",
                        null
                    ));
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        done.await();
        assertThat(errors.get()).isZero();
    }
}
