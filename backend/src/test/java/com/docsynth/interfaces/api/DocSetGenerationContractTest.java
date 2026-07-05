package com.docsynth.interfaces.api;

import com.docsynth.application.documentation.GenerateDocSetUseCase;
import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.documentation.DocSetId;
import com.docsynth.domain.documentation.DocSetRepository;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.security.ProjectRbacFilter;
import com.docsynth.infrastructure.security.TenantContextResolver;
import com.docsynth.interfaces.api.dto.GenerateDocSetRequest;
import com.docsynth.interfaces.api.dto.GenerateDocSetResponse;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract test: POST /api/v1/projects/{projectId}/docsets returns 202 with a
 * docSetId; subsequent GET returns the generated DocSet summary.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocSetGenerationContractTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private GenerateDocSetUseCase useCase;
    @MockBean private DocSetRepository repository;
    @MockBean private TenantContextResolver tenantContext;
    @MockBean private ProjectRbacFilter rbac;

    @Test
    void post_returns_202_with_docset_id() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID docSetId = UUID.randomUUID();
        UUID specId = UUID.randomUUID();

        DocSet ds = DocSet.reconstitute(
            new DocSetId(docSetId), new ProjectId(projectId), new TenantId(UUID.randomUUID()),
            new ApiSpecId(specId), "1.0.0", "v1.0.0/", "https://blob/docsets/v1.0.0/manifest.json",
            true, Instant.now(), null
        );
        when(useCase.execute(any())).thenReturn(ds);

        GenerateDocSetRequest body = new GenerateDocSetRequest(specId, "1.0.0", null);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/projects/{projectId}/docsets", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.docSetId").value(docSetId.toString()));
    }
}
