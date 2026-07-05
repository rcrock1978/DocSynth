package com.docsynth.application.documentation;

import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.documentation.DocSetId;
import com.docsynth.domain.documentation.DocSetRepository;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FR-014: archived → active is not allowed. Archived is terminal.
 */
@SpringBootTest
@ActiveProfiles("test")
class ArchivedReviveIT {

    @Autowired private DocSetStateMachine stateMachine;
    @MockBean private DocSetRepository repository;

    @Test
    void archived_to_active_is_rejected() {
        DocSet archived = sampleArchived();
        assertThatThrownBy(() -> stateMachine.transition(archived, "active", null))
            .isInstanceOf(IllegalStateTransitionException.class)
            .hasMessageContaining("archived");
    }

    private DocSet sampleArchived() {
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
        } catch (Exception e) { throw new RuntimeException(e); }
        return ds;
    }
}
