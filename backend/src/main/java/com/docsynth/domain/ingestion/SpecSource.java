package com.docsynth.domain.ingestion;

import java.net.URI;
import java.util.Objects;

/**
 * SpecSource — value object describing where an OpenAPI spec lives.
 * Three variants: public URL, file upload, GitHub repo with optional token.
 * Per data-model.md §SpecSource + FR-001.
 */
public record SpecSource(String kind, String ref) {

    public static final String URL = "url";
    public static final String FILE = "file_upload";
    public static final String GITHUB = "github_repo";

    public SpecSource {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("SpecSource.kind is required");
        }
        if (ref == null || ref.isBlank()) {
            throw new IllegalArgumentException("SpecSource.ref is required");
        }
        if (!URL.equals(kind) && !FILE.equals(kind) && !GITHUB.equals(kind)) {
            throw new IllegalArgumentException("SpecSource.kind must be one of url|file_upload|github_repo");
        }
        if (URL.equals(kind)) {
            try {
                URI.create(ref).toURL();
            } catch (Exception e) {
                throw new IllegalArgumentException("SpecSource.ref is not a valid URL: " + ref);
            }
        }
        if (GITHUB.equals(kind) && !ref.contains("/")) {
            throw new IllegalArgumentException("GitHub source must be in 'owner/repo[@ref]' form");
        }
    }

    public static SpecSource url(String url) {
        Objects.requireNonNull(url, "url");
        return new SpecSource(URL, url);
    }

    public static SpecSource file(String blobUri) {
        Objects.requireNonNull(blobUri, "blobUri");
        return new SpecSource(FILE, blobUri);
    }

    public static SpecSource githubRepo(String ownerRepo, String ref, String tokenRef) {
        Objects.requireNonNull(ownerRepo, "ownerRepo");
        StringBuilder s = new StringBuilder(ownerRepo);
        if (ref != null && !ref.isBlank()) {
            s.append("@").append(ref);
        }
        return new SpecSource(GITHUB, s.toString());
    }
}
