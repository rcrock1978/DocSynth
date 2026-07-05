package com.docsynth.application.documentation;

import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.documentation.DocSetId;
import com.docsynth.domain.documentation.DocSetRepository;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.documentation.CodeExampleClient;
import com.docsynth.infrastructure.documentation.DescriptionEnhancerClient;
import com.docsynth.infrastructure.documentation.ManifestEmitter;
import com.docsynth.infrastructure.documentation.ViteSsgAdapter;
import com.docsynth.infrastructure.messaging.Outbox;
import com.docsynth.application.audit.AuditEmitter;
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
 * Publishing v2 auto-supersedes the previous v1 active DocSet.
 */
@SpringBootTest
@ActiveProfiles("test")
class SupersedeIT {

    @Autowired private GenerateDocSetUseCase useCase;
    @MockBean private ViteSsgAdapter ssg;
    @MockBean private CodeExampleClient codeClient;
    @MockBean private DescriptionEnhancerClient descClient;
    @MockBean private ManifestEmitter manifestEmitter;
    @MockBean private DocSetRepository repository;
    @MockBean private Outbox outbox;
    @MockBean private AuditEmitter audit;

    @Test
    void publishing_v2_supersedes_v1() {
        when(codeClient.generate(any(), any(), any(), anyString())).thenReturn(
            com.docsynth.proto.documentation.v1.DocGeneratorProto.GenerateCodeExampleResponse.newBuilder()
                .setCode("x").setLanguage("curl").setPromptTemplateVersion("v1.0").setConfidence(0.9f).build()
        );
        when(descClient.enhance(any(), anyString())).thenReturn(
            com.docsynth.proto.documentation.v1.DocGeneratorProto.EnhanceDescriptionResponse.newBuilder()
                .setEnhanced("e").setPromptTemplateVersion("v1.0").setConfidence(0.8f).build()
        );
        when(ssg.build(any(), any())).thenReturn(10);
        when(manifestEmitter.emit(any(), anyString())).thenReturn("https://blob/m.json");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(new GenerateDocSetCommand(
            new ProjectId(UUID.randomUUID()),
            new TenantId(UUID.randomUUID()),
            new ApiSpecId(UUID.randomUUID()),
            "2.0.0",
            List.of("curl")
        ));

        verify(repository, times(1)).supersedePreviousActive(any(), any());
    }
}
