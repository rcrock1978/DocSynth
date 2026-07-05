package com.docsynth.interfaces.api;

import com.docsynth.application.proxy.TryItProxyCommand;
import com.docsynth.application.proxy.TryItProxyUseCase;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.domain.user.UserId;
import com.docsynth.infrastructure.proxy.ProxyResponse;
import com.docsynth.infrastructure.security.TenantContextResolver;
import com.docsynth.interfaces.api.dto.TryItProxyRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * ProxyController — the Try It proxy endpoint.
 * POST /api/v1/proxy/try
 */
@RestController
@RequestMapping("/api/v1/proxy")
public class ProxyController {

    private final TryItProxyUseCase useCase;
    private final TenantContextResolver tenantContext;

    public ProxyController(TryItProxyUseCase useCase, TenantContextResolver tenantContext) {
        this.useCase = useCase;
        this.tenantContext = tenantContext;
    }

    @PostMapping("/try")
    public ProxyResponse execute(
        @RequestParam UUID projectId,
        @RequestParam(required = false) UUID specId,
        @Valid @RequestBody TryItProxyRequest body
    ) {
        TenantId tenantId = tenantContext.currentTenantId();
        UUID userUuid = currentActorId();
        return useCase.execute(new TryItProxyCommand(
            tenantId,
            new UserId(userUuid),
            new ProjectId(projectId),
            specId == null ? null : new ApiSpecId(specId),
            body.proxyToken(),
            body.targetHost(),
            body.targetPort() == 0 ? 443 : body.targetPort(),
            body.method(),
            body.path()
        ));
    }

    private UUID currentActorId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getClaim("user_id").toString());
    }
}
