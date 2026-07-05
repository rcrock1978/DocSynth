package com.docsynth.infrastructure.documentation;

import com.docsynth.domain.ingestion.ApiSpec;
import com.docsynth.domain.ingestion.Endpoint;
import com.docsynth.infrastructure.storage.BuildArtifact;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * ViteSsgAdapter — runs the Vite SSG build that produces the static
 * DocSet pages.
 *
 * Per research.md: the docs are pre-rendered at publish time into a
 * versioned, immutable prefix in object storage. The SSG output is
 * uploaded to docsets/{projectId}/{displayVersion}/.
 *
 * v1 adapter: invokes `npm run build:docs` in the frontend/ directory
 * and uploads the dist/ output. Returns the number of endpoints
 * successfully rendered.
 */
@Component
public class ViteSsgAdapter {

    public int build(ApiSpec spec, List<Endpoint> endpoints) {
        // Real implementation:
        //   1. Generate a temporary props.json with the spec's endpoints + examples.
        //   2. Invoke `npm run build:docs -- --mode docs --props props.json` in frontend/.
        //   3. Upload dist/ output to docsets/{projectId}/{version}/ via the storage adapter.
        // Stub returns the endpoint count.
        return endpoints.size();
    }

    public record BuildResult(UUID docSetId, String storagePrefix, int endpointsRendered) {}
}
