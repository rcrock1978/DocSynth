package com.docsynth.interfaces.api;

import com.docsynth.application.ingestion.IngestSpecCommand;
import com.docsynth.application.ingestion.IngestSpecUseCase;
import com.docsynth.domain.ingestion.ApiSpec;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.ingestion.ApiSpecRepository;
import com.docsynth.domain.ingestion.SpecSource;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.infrastructure.security.ProjectRbacFilter;
import com.docsynth.infrastructure.security.TenantContextResolver;
import com.docsynth.interfaces.api.dto.IngestSpecRequest;
import com.docsynth.interfaces.api.dto.IngestSpecResponse;
import com.docsynth.interfaces.api.dto.SpecSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * SpecController — REST endpoints for spec ingestion (FR-001, FR-002, SC-001).
 *
 *  POST /api/v1/projects/{projectId}/specs        — submit a new spec
 *  GET  /api/v1/projects/{projectId}/specs        — list (paginated)
 *  GET  /api/v1/projects/{projectId}/specs/{id}   — retrieve
 *
 * Per-tenant rate limit: 10 req/min/user (enforced by separate filter;
 * stub for v1). All endpoints require Viewer role minimum; ingestion
 * requires Editor.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/specs")
public class SpecController {

    private final IngestSpecUseCase ingestSpecUseCase;
    private final ApiSpecRepository repository;
    private final TenantContextResolver tenantContext;
    private final ProjectRbacFilter rbac;

    public SpecController(
        IngestSpecUseCase ingestSpecUseCase,
        ApiSpecRepository repository,
        TenantContextResolver tenantContext,
        ProjectRbacFilter rbac
    ) {
        this.ingestSpecUseCase = ingestSpecUseCase;
        this.repository = repository;
        this.tenantContext = tenantContext;
        this.rbac = rbac;
    }

    @PostMapping
    public ResponseEntity<IngestSpecResponse> submit(
        @PathVariable UUID projectId,
        @Valid @RequestBody IngestSpecRequest body
    ) {
        ProjectId pid = new ProjectId(projectId);
        rbac.requireEditor(pid);
        SpecSource source = switch (body.sourceKind()) {
            case "url" -> SpecSource.url(body.sourceRef());
            case "file_upload" -> SpecSource.file(body.sourceRef());
            case "github_repo" -> SpecSource.githubRepo(
                body.sourceRef(), "main", body.accessTokenRef()
            );
            default -> throw new IllegalArgumentException("Invalid sourceKind: " + body.sourceKind());
        };
        UUID actor = currentActorId();
        ApiSpec result = ingestSpecUseCase.execute(new IngestSpecCommand(
            pid,
            tenantContext.currentTenantId(),
            source,
            actor
        ));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new IngestSpecResponse(result.getId().value()));
    }

    @GetMapping
    public List<SpecSummary> list(@PathVariable UUID projectId) {
        rbac.requireViewer(new ProjectId(projectId));
        return repository.findByProjectId(projectId, 50, 0).stream()
            .map(s -> new SpecSummary(
                s.getId().value(),
                s.title(),
                s.specVersion(),
                s.openapiVersion(),
                s.endpointCount(),
                s.schemaCount(),
                s.parsedAt()
            ))
            .toList();
    }

    @GetMapping("/{id}")
    public SpecSummary get(@PathVariable UUID projectId, @PathVariable UUID id) {
        rbac.requireViewer(new ProjectId(projectId));
        return repository.findById(new ApiSpecId(id))
            .map(s -> new SpecSummary(
                s.getId().value(),
                s.title(),
                s.specVersion(),
                s.openapiVersion(),
                s.endpointCount(),
                s.schemaCount(),
                s.parsedAt()
            ))
            .orElseThrow(() -> new IllegalArgumentException("Spec not found: " + id));
    }

    private UUID currentActorId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getClaim("user_id").toString());
    }
}
