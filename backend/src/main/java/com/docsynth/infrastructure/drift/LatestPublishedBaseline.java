package com.docsynth.infrastructure.drift;

import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import org.springframework.stereotype.Component;

/**
 * LatestPublishedBaseline — resolves the baseline spec for drift
 * comparison: the most recent published DocSet's source ApiSpec.
 */
@Component
public class LatestPublishedBaseline {

    public ApiSpecId resolve(ProjectId projectId) {
        // Real implementation: query doc_sets where state = 'active' and
        // project_id = ? order by generated_at desc limit 1; return api_spec_id.
        // v1 stub: returns null to indicate "no baseline yet" (first ingest).
        return null;
    }
}
