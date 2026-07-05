package com.docsynth.application.drift;

import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;

import java.util.UUID;

public record DetectDriftCommand(
    ProjectId projectId,
    TenantId tenantId,
    ApiSpecId leftSpecId,
    String trigger,
    UUID actorUserId
) {}
