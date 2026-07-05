package com.docsynth.interfaces.api;

import com.docsynth.application.drift.DetectDriftCommand;
import com.docsynth.application.drift.DetectDriftUseCase;
import com.docsynth.domain.drift.DriftItem;
import com.docsynth.domain.drift.DriftReport;
import com.docsynth.domain.drift.DriftReportId;
import com.docsynth.domain.drift.DriftReportRepository;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.infrastructure.security.ProjectRbacFilter;
import com.docsynth.infrastructure.security.TenantContextResolver;
import com.docsynth.interfaces.api.dto.DriftReportSummary;
import com.docsynth.interfaces.api.dto.DriftTriggerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * DriftController — list, trigger, retrieve drift reports.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/drift")
public class DriftController {

    private final DetectDriftUseCase useCase;
    private final DriftReportRepository repository;
    private final TenantContextResolver tenantContext;
    private final ProjectRbacFilter rbac;

    public DriftController(
        DetectDriftUseCase useCase,
        DriftReportRepository repository,
        TenantContextResolver tenantContext,
        ProjectRbacFilter rbac
    ) {
        this.useCase = useCase;
        this.repository = repository;
        this.tenantContext = tenantContext;
        this.rbac = rbac;
    }

    @PostMapping
    public ResponseEntity<DriftReportSummary> trigger(
        @PathVariable UUID projectId,
        @Valid @RequestBody DriftTriggerRequest body
    ) {
        ProjectId pid = new ProjectId(projectId);
        rbac.requireEditor(pid);
        DriftReport result = useCase.execute(new DetectDriftCommand(
            pid,
            tenantContext.currentTenantId(),
            new ApiSpecId(body.specId()),
            body.trigger() == null ? "manual" : body.trigger(),
            currentActorId()
        ));
        if (result == null) {
            return ResponseEntity.accepted().body(null);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(toSummary(result));
    }

    @GetMapping
    public List<DriftReportSummary> list(@PathVariable UUID projectId) {
        rbac.requireViewer(new ProjectId(projectId));
        return repository.findByProjectId(projectId, 50, 0).stream()
            .map(this::toSummary)
            .toList();
    }

    @GetMapping("/{id}")
    public DriftReportSummary get(@PathVariable UUID projectId, @PathVariable UUID id) {
        rbac.requireViewer(new ProjectId(projectId));
        return repository.findById(new DriftReportId(id))
            .map(this::toSummary)
            .orElseThrow(() -> new IllegalArgumentException("Drift report not found: " + id));
    }

    @GetMapping("/{id}/items")
    public List<DriftItem> items(@PathVariable UUID projectId, @PathVariable UUID id) {
        rbac.requireViewer(new ProjectId(projectId));
        return repository.findItemsByReportId(new DriftReportId(id), tenantContext.currentTenantId());
    }

    private DriftReportSummary toSummary(DriftReport r) {
        return new DriftReportSummary(
            r.getId().value(),
            r.getTrigger(),
            r.summary().added(),
            r.summary().removed(),
            r.summary().changed(),
            r.summary().breaking(),
            r.getNotificationStatus(),
            r.getGeneratedAt()
        );
    }

    private UUID currentActorId() {
        var jwt = (org.springframework.security.oauth2.jwt.Jwt)
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getClaim("user_id").toString());
    }
}
