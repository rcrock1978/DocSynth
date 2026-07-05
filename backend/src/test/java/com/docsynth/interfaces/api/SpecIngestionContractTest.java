package com.docsynth.interfaces.api;

import com.docsynth.domain.ingestion.ApiSpec;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.ingestion.ApiSpecRepository;
import com.docsynth.domain.ingestion.Endpoint;
import com.docsynth.domain.ingestion.SpecSource;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.parsing.SwaggerParserAdapter;
import com.docsynth.interfaces.api.dto.IngestSpecRequest;
import com.docsynth.interfaces.api.dto.IngestSpecResponse;
import com.docsynth.application.ingestion.IngestSpecUseCase;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract test: POST /api/v1/projects/{projectId}/specs accepts a URL source
 * and returns 201 Created with a specId in the body. Per SC-001 the parsed
 * representation should be available within 30 s.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpecIngestionContractTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IngestSpecUseCase ingestSpecUseCase;

    @MockBean
    private ApiSpecRepository apiSpecRepository;

    @Test
    void accepts_url_source_and_returns_201() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID specId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        ApiSpec spec = ApiSpec.reconstitute(
            new ApiSpecId(specId),
            new ProjectId(projectId),
            new TenantId(tenantId),
            SpecSource.url("https://example.com/openapi.json"),
            "3.0.3",
            "https://blob/docsynth/specs/" + specId + ".yaml",
            "abc123",
            "Petstore",
            "1.0.0",
            50,
            12,
            Instant.now(),
            null
        );
        when(ingestSpecUseCase.execute(any())).thenReturn(spec);
        when(apiSpecRepository.findById(new ApiSpecId(specId))).thenReturn(Optional.of(spec));

        IngestSpecRequest request = new IngestSpecRequest("url", "https://example.com/openapi.json", null);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/projects/{projectId}/specs", projectId)
                .with(req -> { req.setRemoteAddr("127.0.0.1"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.specId").value(specId.toString()));
    }
}
