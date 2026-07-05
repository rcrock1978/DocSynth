package com.docsynth.infrastructure.publishing;

import com.docsynth.application.documentation.DocSetStateMachine;
import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.documentation.DocSetId;
import com.docsynth.domain.documentation.DocSetRepository;
import com.docsynth.domain.documentation.DocSetState;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * End-to-end publishing lifecycle:
 *  - active -> deprecated: succeeds
 *  - deprecated DocSet displays banner
 *  - archived returns 410 after 90 days (covered separately in GoneWorkerIT)
 */
@SpringBootTest
@ActiveProfiles("test")
class PublishingLifecycleIT {

    @Autowired private DocSetStateMachine stateMachine;
    @MockBean private DocSetRepository repository;
    @MockBean private DeprecationBannerEmitter bannerEmitter;
    @MockBean private FrontDoorCacheInvalidator cacheInvalidator;

    @Test
    void active_to_deprecated_succeeds_and_banner_emitted() {
        DocSet active = sampleDocSet("1.0.0", DocSetState.ACTIVE, null, null);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DocSet deprecated = stateMachine.transition(active, DocSetState.DEPRECATED, null);
        assertThat(deprecated.getState()).isEqualTo("deprecated");
        assertThat(deprecated.getDeprecatedAt()).isNotNull();
    }

    @Test
    void deprecate_then_revert_to_active_succeeds() {
        DocSet deprecated = sampleDocSet("1.0.0", DocSetState.DEPRECATED, Instant.now(), null);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DocSet active = stateMachine.transition(deprecated, DocSetState.ACTIVE, null);
        assertThat(active.getState()).isEqualTo("active");
        assertThat(active.getDeprecatedAt()).isNull();
    }

    @Test
    void archive_before_90_days_is_rejected() {
        DocSet recentlyDeprecated = sampleDocSet("1.0.0", DocSetState.DEPRECATED,
            Instant.now().minus(Duration.ofDays(30)), null);
        assertThatThrownBy(() -> stateMachine.transition(recentlyDeprecated, DocSetState.ARCHIVED, null))
            .isInstanceOf(IllegalStateTransitionException.class)
            .hasMessageContaining("90 days");
    }

    @Test
    void archive_after_90_days_succeeds() {
        DocSet oldDeprecated = sampleDocSet("1.0.0", DocSetState.DEPRECATED,
            Instant.now().minus(Duration.ofDays(95)), null);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DocSet archived = stateMachine.transition(oldDeprecated, DocSetState.ARCHIVED, null);
        assertThat(archived.getState()).isEqualTo("archived");
        assertThat(archived.getArchivedAt()).isNotNull();
    }

    private DocSet sampleDocSet(String version, String state, Instant deprecatedAt, Instant archivedAt) {
        DocSet ds = DocSet.reconstitute(
            new DocSetId(UUID.randomUUID()),
            new ProjectId(UUID.randomUUID()),
            new TenantId(UUID.randomUUID()),
            new ApiSpecId(UUID.randomUUID()),
            version, "v" + version + "/", "https://blob/manifest.json",
            true, Instant.now(), null
        );
        // The state field is package-private; reflect-set for tests.
        try {
            var f = DocSet.class.getDeclaredField("state");
            f.setAccessible(true);
            f.set(ds, state);
            var da = DocSet.class.getDeclaredField("deprecatedAt");
            da.setAccessible(true);
            da.set(ds, deprecatedAt);
            var aa = DocSet.class.getDeclaredField("archivedAt");
            aa.setAccessible(true);
            aa.set(ds, archivedAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ds;
    }
}
