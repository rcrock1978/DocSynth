package com.docsynth.application.proxy;

import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.domain.user.UserId;

import java.util.UUID;

public record TryItProxyCommand(
    TenantId tenantId,
    UserId userId,
    ProjectId projectId,
    ApiSpecId specId,
    String proxyToken,
    String targetHost,
    int targetPort,
    String method,
    String path
) {}
