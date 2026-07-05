package com.docsynth.interfaces.api;

import com.docsynth.application.documentation.DocSetStateMachine;
import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.documentation.DocSetId;
import com.docsynth.domain.documentation.DocSetRepository;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.security.ProjectRbacFilter;
import com.docsynth.infrastructure.security.TenantContextResolver;
import com.docsynth.interfaces.api.dto.TransitionDocSetStateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract test: PATCH /api/v1/projects/{projectId}/docsets/{docSetId}/state
 * enforces FR-014 transition rules.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocSetStateContractTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private DocSetStateMachine stateMachine;
    @MockBean private DocSetRepository repository;
    @MockBean private TenantContextResolver tenantContext;
    @MockBean private ProjectRbacFilter rbac;

    @Test
    void deprecate_transition_succeeds() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID docSetId = UUID.randomUUID();

        DocSet ds = DocSet.reconstitute(
            new DocSetId(docSetId), new ProjectId(projectId), new TenantId(UUID.randomUUID()),
            new ApiSpecId(UUID.randomUUID()), "1.0.0", "v1.0.0/", "https://blob/manifest.json",
            true, Instant.now(), null
        );
        when(stateMachine.transition(any(), any(), any())).thenReturn(ds);

        TransitionDocSetStateRequest body = new TransitionDocSetStateRequest("deprecate", null);

        mvc.perform(MockMvcRequestBuilders.patch(
                "/api/v1/projects/{projectId}/docsets/{docSetId}/state", projectId, docSetId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk());
    }
}
