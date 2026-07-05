package com.docsynth.interfaces.api;

import com.docsynth.application.drift.DetectDriftUseCase;
import com.docsynth.domain.drift.DriftReport;
import com.docsynth.domain.drift.DriftReportId;
import com.docsynth.domain.drift.DriftReportRepository;
import com.docsynth.domain.drift.DriftSummary;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.security.ProjectRbacFilter;
import com.docsynth.infrastructure.security.TenantContextResolver;
import com.docsynth.interfaces.api.dto.DriftTriggerRequest;
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
 * Contract test: POST /api/v1/projects/{projectId}/drift triggers a
 * comparison and returns 202 with a driftReportId.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DriftContractTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private DetectDriftUseCase useCase;
    @MockBean private DriftReportRepository repository;
    @MockBean private TenantContextResolver tenantContext;
    @MockBean private ProjectRbacFilter rbac;

    @Test
    void post_returns_202_with_report_id() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        DriftReport report = DriftReport.reconstitute(
            new DriftReportId(reportId), new ProjectId(projectId), new TenantId(UUID.randomUUID()),
            null, null, "manual", null,
            new DriftSummary(0, 0, 1, 0),
            Instant.now(), null, "pending"
        );
        when(useCase.execute(any())).thenReturn(report);

        DriftTriggerRequest body = new DriftTriggerRequest(UUID.randomUUID(), "manual");

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/projects/{projectId}/drift", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.driftReportId").value(reportId.toString()));
    }
}
