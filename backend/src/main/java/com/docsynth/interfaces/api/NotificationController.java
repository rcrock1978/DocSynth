package com.docsynth.interfaces.api;

import com.docsynth.domain.drift.NotificationChannel;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.persistence.NotificationChannelJpaRepository;
import com.docsynth.infrastructure.security.ProjectRbacFilter;
import com.docsynth.infrastructure.security.TenantContextResolver;
import com.docsynth.interfaces.api.dto.CreateChannelRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * NotificationController — CRUD on per-project notification channels.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/channels")
public class NotificationController {

    private final NotificationChannelJpaRepository repository;
    private final TenantContextResolver tenantContext;
    private final ProjectRbacFilter rbac;

    public NotificationController(
        NotificationChannelJpaRepository repository,
        TenantContextResolver tenantContext,
        ProjectRbacFilter rbac
    ) {
        this.repository = repository;
        this.tenantContext = tenantContext;
        this.rbac = rbac;
    }

    @PostMapping
    public ResponseEntity<NotificationChannel> create(
        @PathVariable UUID projectId,
        @RequestBody CreateChannelRequest body
    ) {
        ProjectId pid = new ProjectId(projectId);
        rbac.requireEditor(pid);
        NotificationChannel ch = new NotificationChannel(
            pid, tenantContext.currentTenantId(),
            body.kind(), body.name(), body.configRef()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(ch));
    }

    @GetMapping
    public List<NotificationChannel> list(@PathVariable UUID projectId) {
        rbac.requireViewer(new ProjectId(projectId));
        return repository.findByProjectId(projectId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID projectId, @PathVariable UUID id) {
        rbac.requireEditor(new ProjectId(projectId));
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
