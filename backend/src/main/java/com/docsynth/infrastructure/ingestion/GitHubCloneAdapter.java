package com.docsynth.infrastructure.ingestion;

import com.docsynth.infrastructure.security.KeyVaultSecretStore;
import org.springframework.stereotype.Component;

/**
 * GitHubCloneAdapter — detects the spec file within a GitHub repo.
 * v1 implementation: probes a fixed list of well-known spec file paths
 * via raw.githubusercontent.com (no clone needed).
 */
@Component
public class GitHubCloneAdapter {

    private static final String[] CANDIDATE_PATHS = {
        "openapi.yaml", "openapi.yml", "openapi.json",
        "api/openapi.yaml", "api/openapi.yml", "api/openapi.json",
        "docs/openapi.yaml", "docs/openapi.yml", "docs/openapi.json",
        "spec/openapi.yaml", "spec/openapi.yml", "spec/openapi.json"
    };

    private final UrlSpecDownloader downloader;
    private final KeyVaultSecretStore secrets;

    public GitHubCloneAdapter(UrlSpecDownloader downloader, KeyVaultSecretStore secrets) {
        this.downloader = downloader;
        this.secrets = secrets;
    }

    /**
     * @return the first spec file path (relative to repo root) that resolves
     *         to a non-error response on raw.githubusercontent.com.
     */
    public String detectSpecFile(String ownerRepo, String ref) {
        for (String candidate : CANDIDATE_PATHS) {
            String url = String.format(
                "https://raw.githubusercontent.com/%s/%s/%s",
                ownerRepo, ref, candidate
            );
            try {
                downloader.download(SpecSourceShim.url(url));
                return candidate;
            } catch (Exception ignored) {
                // Try next candidate.
            }
        }
        throw new com.docsynth.application.ingestion.InvalidSpecException(
            "No OpenAPI spec file found in repo " + ownerRepo + "@" + ref
                + " (probed " + CANDIDATE_PATHS.length + " common paths)"
        );
    }

    /** Local shim to avoid a circular import. */
    private static final class SpecSourceShim {
        static com.docsynth.domain.ingestion.SpecSource url(String url) {
            return com.docsynth.domain.ingestion.SpecSource.url(url);
        }
    }
}
