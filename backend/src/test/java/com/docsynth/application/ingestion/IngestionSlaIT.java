package com.docsynth.application.ingestion;

import com.docsynth.domain.ingestion.ApiSpec;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.ingestion.SpecSource;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.ingestion.BlobSpecStorage;
import com.docsynth.infrastructure.ingestion.UrlSpecDownloader;
import com.docsynth.infrastructure.parsing.SwaggerParserAdapter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.converter.SwaggerConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test for SC-001: a spec with 50 endpoints is parsed and made
 * available within 30 seconds of submission.
 */
@SpringBootTest
@ActiveProfiles("test")
class IngestionSlaIT {

    @Autowired
    private IngestSpecUseCase useCase;

    @MockBean
    private UrlSpecDownloader downloader;

    @MockBean
    private BlobSpecStorage storage;

    @Test
    void fifty_endpoints_ingested_within_30s() throws Exception {
        OpenAPI spec = new OpenAPI();
        Info info = new Info();
        info.setTitle("Test API");
        info.setVersion("1.0.0");
        spec.setInfo(info);
        Map<String, PathItem> paths = new HashMap<>();
        for (int i = 0; i < 50; i++) {
            PathItem p = new PathItem();
            p.setGet(new io.swagger.v3.oas.models.Operation()
                .operationId("op" + i)
                .summary("Op " + i));
            paths.put("/resource/" + i, p);
        }
        spec.setPaths(paths);

        when(downloader.download(any())).thenReturn("openapi: 3.0.3\ninfo:\n  title: Test\n  version: 1.0.0");
        when(storage.store(any(), any())).thenReturn("https://blob/specs/abc.yaml");

        Instant start = Instant.now();
        ApiSpec result = useCase.execute(new IngestSpecCommand(
            new ProjectId(java.util.UUID.randomUUID()),
            new TenantId(java.util.UUID.randomUUID()),
            SpecSource.url("https://example.com/openapi.json"),
            null
        ));
        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(result).isNotNull();
        assertThat(result.endpointCount()).isEqualTo(50);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(30));
    }
}
