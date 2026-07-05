package com.docsynth.application.ingestion;

import com.docsynth.domain.ingestion.SpecSource;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;

import java.util.UUID;

public record IngestSpecCommand(
    ProjectId projectId,
    TenantId tenantId,
    SpecSource source,
    UUID actorUserId
) {}
