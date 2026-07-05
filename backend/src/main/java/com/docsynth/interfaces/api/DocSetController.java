package com.docsynth.interfaces.api;

import com.docsynth.application.documentation.GenerateDocSetCommand;
import com.docsynth.application.documentation.GenerateDocSetUseCase;
import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.documentation.DocSetId;
import com.docsynth.domain.documentation.DocSetRepository;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.infrastructure.security.ProjectRbacFilter;
import com.docsynth.infrastructure.security.TenantContextResolver;
import com.docsynth.interfaces.api.dto.DocSetSummary;
import com.docsynth.interfaces.api.dto.GenerateDocSetRequest;
import com.docsynth.interfaces.api.dto.GenerateDocSetResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * DocSetController — generate, list, retrieve DocSets.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/docsets")
public class DocSetController {

    private final GenerateDocSetUseCase useCase;
    private final DocSetRepository repository;
    private final TenantContextResolver tenantContext;
    private final ProjectRbacFilter rbac;

    public DocSetController(
        GenerateDocSetUseCase useCase,
        DocSetRepository repository,
        TenantContextResolver tenantContext,
        ProjectRbacFilter rbac
    ) {
        this.useCase = useCase;
        this.repository = repository;
        this.tenantContext = tenantContext;
        this.rbac = rbac;
    }

    @PostMapping
    public ResponseEntity<GenerateDocSetResponse> generate(
        @PathVariable UUID projectId,
        @Valid @RequestBody GenerateDocSetRequest body
    ) {
        ProjectId pid = new ProjectId(projectId);
        rbac.requireEditor(pid);
        List<String> languages = body.targetLanguages() == null || body.targetLanguages().isEmpty()
            ? List.of("curl", "python", "java")
            : body.targetLanguages();
        DocSet result = useCase.execute(new GenerateDocSetCommand(
            pid,
            tenantContext.currentTenantId(),
            new ApiSpecId(body.specId()),
            body.displayVersion(),
            languages
        ));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(new GenerateDocSetResponse(result.getId().value()));
    }

    @GetMapping
    public List<DocSetSummary> list(@PathVariable UUID projectId) {
        rbac.requireViewer(new ProjectId(projectId));
        return repository.findByProjectId(projectId, 50, 0).stream()
            .map(this::toSummary)
            .toList();
    }

    @GetMapping("/{id}")
    public DocSetSummary get(@PathVariable UUID projectId, @PathVariable UUID id) {
        rbac.requireViewer(new ProjectId(projectId));
        return repository.findById(new DocSetId(id))
            .map(this::toSummary)
            .orElseThrow(() -> new IllegalArgumentException("DocSet not found: " + id));
    }

    private DocSetSummary toSummary(DocSet d) {
        return new DocSetSummary(
            d.getId().value(),
            d.getDisplayVersion(),
            d.getState(),
            d.isTryItEnabled(),
            d.getGeneratedAt(),
            d.getPublishedAt(),
            d.getDeprecatedAt(),
            d.getArchivedAt(),
            d.getGoneAt()
        );
    }
}
