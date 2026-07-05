package com.docsynth.application.documentation;

import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.documentation.DocSetId;
import com.docsynth.domain.documentation.DocSetRepository;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.publishing.DeprecationBannerEmitter;
import com.docsynth.infrastructure.publishing.FrontDoorCacheInvalidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * SC-007 / FR-014: 90-day Gone worker flips archived DocSets' `gone_at`
 * and updates the manifest so the CDN serves 410.html.
 */
@SpringBootTest
@ActiveProfiles("test")
class GoneWorkerIT {

    @Autowired private GoneWorker worker;
    @MockBean private DocSetRepository repository;
    @MockBean private DeprecationBannerEmitter bannerEmitter;
    @MockBean private FrontDoorCacheInvalidator cacheInvalidator;

    @Test
    void archived_older_than_90_days_gets_gone_at_set() {
        DocSet oldArchived = sampleArchived(Duration.ofDays(95));
        when(repository.findArchivedOlderThan(any())).thenReturn(List.of(oldArchived));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int processed = worker.runOnce();
        assertThat(processed).isEqualTo(1);
        assertThat(oldArchived.getGoneAt()).isNotNull();
    }

    @Test
    void archived_younger_than_90_days_is_skipped() {
        DocSet recentArchived = sampleArchived(Duration.ofDays(30));
        when(repository.findArchivedOlderThan(any())).thenReturn(List.of());
        int processed = worker.runOnce();
        assertThat(processed).isEqualTo(0);
    }

    private DocSet sampleArchived(Duration archivedAgo) {
        DocSet ds = DocSet.reconstitute(
            new DocSetId(UUID.randomUUID()),
            new ProjectId(UUID.randomUUID()),
            new TenantId(UUID.randomUUID()),
            new ApiSpecId(UUID.randomUUID()),
            "1.0.0", "v1.0.0/", "https://blob/m.json",
            true, Instant.now(), null
        );
        try {
            var f = DocSet.class.getDeclaredField("state");
            f.setAccessible(true);
            f.set(ds, "archived");
            var aa = DocSet.class.getDeclaredField("archivedAt");
            aa.setAccessible(true);
            aa.set(ds, Instant.now().minus(archivedAgo));
        } catch (Exception e) { throw new RuntimeException(e); }
        return ds;
    }
}
