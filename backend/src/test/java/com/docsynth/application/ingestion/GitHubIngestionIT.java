package com.docsynth.application.ingestion;

import com.docsynth.domain.ingestion.SpecSource;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.ingestion.BlobSpecStorage;
import com.docsynth.infrastructure.ingestion.GitHubCloneAdapter;
import com.docsynth.infrastructure.ingestion.UrlSpecDownloader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration test: GitHub repo source clones, detects the spec file
 * (openapi.yaml at repo root or under a common docs/ path), and parses it.
 */
@SpringBootTest
@ActiveProfiles("test")
class GitHubIngestionIT {

    @Autowired
    private IngestSpecUseCase useCase;

    @MockBean
    private UrlSpecDownloader downloader;

    @MockBean
    private BlobSpecStorage storage;

    @MockBean
    private GitHubCloneAdapter gitHubClone;

    @Test
    void github_repo_with_openapi_yaml_is_ingested() throws Exception {
        when(gitHubClone.detectSpecFile(anyString(), anyString())).thenReturn("openapi.yaml");
        when(downloader.download(any())).thenReturn("openapi: 3.0.3\ninfo:\n  title: GH API\n  version: 1.0.0\npaths:\n  /users:\n    get:\n      summary: List users");
        when(storage.store(any(), any())).thenReturn("https://blob/specs/gh.yaml");

        var result = useCase.execute(new IngestSpecCommand(
            new ProjectId(java.util.UUID.randomUUID()),
            new TenantId(java.util.UUID.randomUUID()),
            SpecSource.githubRepo("owner/repo", "main", "kv://github-token"),
            null
        ));

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("GH API");
        assertThat(result.endpointCount()).isGreaterThanOrEqualTo(1);
    }
}
