package com.docsynth.infrastructure.documentation;

import com.docsynth.domain.ingestion.ApiSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ManifestEmitter — writes the DocSet manifest (index.json) that drives
 * the version selector and the SSG bundle's per-version metadata.
 *
 * Per research.md §Storage layout:
 *   index.json — list of versions, status, per-version metadata.
 *   The manifest is the single source of truth for "which versions
 *   exist, which is current, which is deprecated, where do 410s live."
 */
@Component
public class ManifestEmitter {

    private final ObjectMapper objectMapper;

    public ManifestEmitter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String emit(ApiSpec spec, String displayVersion) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schemaVersion", 1);
        manifest.put("specSha256", spec.specSha256());
        manifest.put("openapiVersion", spec.openapiVersion());
        manifest.put("title", spec.title());
        manifest.put("specVersion", spec.specVersion());
        manifest.put("displayVersion", displayVersion);
        manifest.put("generatedAt", Instant.now().toString());
        manifest.put("versions", List.of(Map.of(
            "displayVersion", displayVersion,
            "state", "active",
            "storagePrefix", "v" + displayVersion + "/"
        )));
        try {
            String json = objectMapper.writeValueAsString(manifest);
            // Real implementation: upload to docsets/{projectId}/index.json.
            return "https://docsynth.blob.core.windows.net/docsets/"
                + spec.getProjectId().value() + "/index.json";
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize manifest", e);
        }
    }
}
