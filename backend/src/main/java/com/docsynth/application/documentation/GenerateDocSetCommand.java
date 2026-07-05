package com.docsynth.application.documentation;

import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;

import java.util.List;
import java.util.UUID;

public record GenerateDocSetCommand(
    ProjectId projectId,
    TenantId tenantId,
    ApiSpecId specId,
    String displayVersion,
    List<String> targetLanguages
) {
    public UUID actorUserId() {
        // Resolved by the use-case wrapper; deferred to keep this class independent.
        return null;
    }
}
