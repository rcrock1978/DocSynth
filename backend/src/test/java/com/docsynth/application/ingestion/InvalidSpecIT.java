package com.docsynth.application.ingestion;

import com.docsynth.domain.ingestion.SpecSource;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.ingestion.BlobSpecStorage;
import com.docsynth.infrastructure.ingestion.UrlSpecDownloader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test: invalid/malformed spec URLs return a clear error
 * describing what failed and where.
 */
@SpringBootTest
@ActiveProfiles("test")
class InvalidSpecIT {

    @Autowired
    private IngestSpecUseCase useCase;

    @MockBean
    private UrlSpecDownloader downloader;

    @MockBean
    private BlobSpecStorage storage;

    @Test
    void malformed_url_is_rejected_with_clear_error() {
        assertThatThrownBy(() ->
            useCase.execute(new IngestSpecCommand(
                new ProjectId(java.util.UUID.randomUUID()),
                new TenantId(java.util.UUID.randomUUID()),
                SpecSource.url("not a valid url"),
                null
            )))
            .isInstanceOf(InvalidSpecException.class)
            .hasMessageContaining("url");
    }

    @Test
    void download_failure_propagates_with_status_code() {
        when(downloader.download(any())).thenThrow(new SpecDownloadException("HTTP 404 from https://example.com/missing.json"));
        assertThatThrownBy(() ->
            useCase.execute(new IngestSpecCommand(
                new ProjectId(java.util.UUID.randomUUID()),
                new TenantId(java.util.UUID.randomUUID()),
                SpecSource.url("https://example.com/missing.json"),
                null
            )))
            .isInstanceOf(SpecDownloadException.class)
            .hasMessageContaining("404");
    }
}
