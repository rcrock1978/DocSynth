package com.docsynth.infrastructure.ingestion;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * BlobSpecStorage — writes raw spec text to immutable Blob Storage.
 * Key: specs/{projectId}/{uuid}.yaml
 */
@Component
public class BlobSpecStorage {

    /** @return the storage URI for the stored spec. */
    public String store(String projectId, String specText) {
        String key = String.format("specs/%s/%s.yaml", projectId, UUID.randomUUID());
        // Real implementation: Azure Blob Storage client. Stub returns a
        // synthetic URI; downstream consumers read the raw spec from the
        // ApiSpec.rawSpecUri field.
        return "https://docsynth.blob.core.windows.net/specs/" + key;
    }
}
